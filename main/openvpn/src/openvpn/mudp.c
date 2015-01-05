/*
 *  OpenVPN -- An application to securely tunnel IP networks
 *             over a single TCP/UDP port, with support for SSL/TLS-based
 *             session authentication and key exchange,
 *             packet encryption, packet authentication, and
 *             packet compression.
 *
 *  Copyright (C) 2002-2010 OpenVPN Technologies, Inc. <sales@openvpn.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program (see the file COPYING included with this
 *  distribution); if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#elif defined(_MSC_VER)
#include "config-msvc.h"
#endif

#include "syshead.h"

#if P2MP_SERVER

#include "multi.h"
#include "forward-inline.h"

#include "memdbg.h"

/*
 * Update instance with new peer address
 */
void
update_floated(struct multi_context *m, struct multi_instance *mi,
	       struct mroute_addr real, uint32_t hv)
{
  struct mroute_addr real_old;

  real_old = mi->real;
  generate_prefix (mi);

  /* remove before modifying mi->real, since it also modifies key in hash */
  hash_remove(m->hash, &real_old);
  hash_remove(m->iter, &real_old);

  /* update address */
  memcpy(&mi->real, &real, sizeof(real));

  mi->context.c2.from = m->top.c2.from;
  mi->context.c2.to_link_addr = &mi->context.c2.from;

  /* switch to new log prefix */
  generate_prefix (mi);
  /* inherit buffers */
  mi->context.c2.buffers = m->top.c2.buffers;

  /* inherit parent link_socket and link_socket_info */
  mi->context.c2.link_socket = m->top.c2.link_socket;
  mi->context.c2.link_socket_info->lsa->actual = m->top.c2.from;

  /* fix remote_addr in tls structure */
  tls_update_remote_addr (mi->context.c2.tls_multi, &mi->context.c2.from);
  mi->did_open_context = true;

  hash_add(m->hash, &mi->real, mi, false);
  hash_add(m->iter, &mi->real, mi, false);

  mi->did_real_hash = true;
#ifdef MANAGEMENT_DEF_AUTH
  hash_remove (m->cid_hash, &mi->context.c2.mda_context.cid);
  hash_add (m->cid_hash, &mi->context.c2.mda_context.cid, mi, false);
#endif

#ifdef MANAGEMENT_DEF_AUTH
  mi->did_cid_hash = true;
#endif
}

/*
 * Get a client instance based on real address.  If
 * the instance doesn't exist, create it while
 * maintaining real address hash table atomicity.
 */

struct multi_instance *
multi_get_create_instance_udp (struct multi_context *m)
{
  struct gc_arena gc = gc_new ();
  struct mroute_addr real;
  struct multi_instance *mi = NULL;
  struct hash *hash = m->hash;

  if (mroute_extract_openvpn_sockaddr (&real, &m->top.c2.from.dest, true))
    {
      struct hash_element *he;
      const uint32_t hv = hash_value (hash, &real);
      struct hash_bucket *bucket = hash_bucket (hash, hv);
      uint8_t* ptr  = BPTR(&m->top.c2.buf);
      uint8_t op = ptr[0] >> P_OPCODE_SHIFT;
      uint32_t sess_id;
      bool session_forged = false;

      if (op == P_DATA_V2)
	{
	  sess_id = (*(uint32_t*)ptr) >> 8;
	  if ((sess_id < m->max_clients) && (m->instances[sess_id]))
	    {
	      mi = m->instances[sess_id];

	      if (!link_socket_actual_match(&mi->context.c2.from, &m->top.c2.from))
		{
		  msg(D_MULTI_MEDIUM, "floating detected from %s to %s",
		      print_link_socket_actual (&mi->context.c2.from, &gc), print_link_socket_actual (&m->top.c2.from, &gc));

		  /* session-id is not trusted, so check hmac */
		  session_forged = !(crypto_test_hmac(&m->top.c2.buf, &mi->context.c2.crypto_options));
		  if (session_forged)
		    {
		      mi = NULL;
		      msg (D_MULTI_MEDIUM, "hmac verification failed, session forge detected!");
		    }
		  else
		    {
		      update_floated(m, mi, real, hv);
		    }
		}
	    }
	}
      else
	{
	  he = hash_lookup_fast (hash, bucket, &real, hv);
	  if (he)
	    {
	      mi = (struct multi_instance *) he->value;
	    }
	}
      if (!mi && !session_forged)
	{
	  if (!m->top.c2.tls_auth_standalone
	      || tls_pre_decrypt_lite (m->top.c2.tls_auth_standalone, &m->top.c2.from, &m->top.c2.buf))
	    {
	      if (frequency_limit_event_allowed (m->new_connection_limiter))
		{
		  mi = multi_create_instance (m, &real);
		  if (mi)
		    {
		      hash_add_fast (hash, bucket, &mi->real, hv, mi);
		      mi->did_real_hash = true;

		      int i;
		      for (i = 0; i < m->max_clients; ++ i)
			{
			  if (!m->instances[i])
			    {
			      mi->context.c2.tls_multi->vpn_session_id = i;
			      m->instances[i] = mi;
			      break;
			    }
			}
		    }
		}
	      else
		{
		  msg (D_MULTI_ERRORS,
		       "MULTI: Connection from %s would exceed new connection frequency limit as controlled by --connect-freq",
		       mroute_addr_print (&real, &gc));
		}
	    }
	}

#ifdef ENABLE_DEBUG
      if (check_debug_level (D_MULTI_DEBUG))
	{
	  const char *status = mi ? "[ok]" : "[failed]";

	  /*
	  if (he && mi)
	    status = "[succeeded]";
	  else if (!he && mi)
	    status = "[created]";
	  else
	    status = "[failed]";
	  */

	  dmsg (D_MULTI_DEBUG, "GET INST BY REAL: %s %s",
	       mroute_addr_print (&real, &gc),
	       status);
	}
#endif
    }

  gc_free (&gc);
  ASSERT (!(mi && mi->halt));
  return mi;
}

/*
 * Send a packet to TCP/UDP socket.
 */
static inline void
multi_process_outgoing_link (struct multi_context *m, const unsigned int mpp_flags)
{
  struct multi_instance *mi = multi_process_outgoing_link_pre (m);
  if (mi)
    multi_process_outgoing_link_dowork (m, mi, mpp_flags);
}

/*
 * Process an I/O event.
 */
static void
multi_process_io_udp (struct multi_context *m)
{
  const unsigned int status = m->top.c2.event_set_status;
  const unsigned int mpp_flags = m->top.c2.fast_io
    ? (MPP_CONDITIONAL_PRE_SELECT | MPP_CLOSE_ON_SIGNAL)
    : (MPP_PRE_SELECT | MPP_CLOSE_ON_SIGNAL);

#ifdef MULTI_DEBUG_EVENT_LOOP
  char buf[16];
  buf[0] = 0;
  if (status & SOCKET_READ)
    strcat (buf, "SR/");
  else if (status & SOCKET_WRITE)
    strcat (buf, "SW/");
  else if (status & TUN_READ)
    strcat (buf, "TR/");
  else if (status & TUN_WRITE)
    strcat (buf, "TW/");
  printf ("IO %s\n", buf);
#endif

#ifdef ENABLE_MANAGEMENT
  if (status & (MANAGEMENT_READ|MANAGEMENT_WRITE))
    {
      ASSERT (management);
      management_io (management);
    }
#endif

  /* UDP port ready to accept write */
  if (status & SOCKET_WRITE)
    {
      multi_process_outgoing_link (m, mpp_flags);
    }
  /* TUN device ready to accept write */
  else if (status & TUN_WRITE)
    {
      multi_process_outgoing_tun (m, mpp_flags);
    }
  /* Incoming data on UDP port */
  else if (status & SOCKET_READ)
    {
      read_incoming_link (&m->top);
      multi_release_io_lock (m);
      if (!IS_SIG (&m->top))
	multi_process_incoming_link (m, NULL, mpp_flags);
    }
  /* Incoming data on TUN device */
  else if (status & TUN_READ)
    {
      read_incoming_tun (&m->top);
      multi_release_io_lock (m);
      if (!IS_SIG (&m->top))
	multi_process_incoming_tun (m, mpp_flags);
    }
}

/*
 * Return the io_wait() flags appropriate for
 * a point-to-multipoint tunnel.
 */
static inline unsigned int
p2mp_iow_flags (const struct multi_context *m)
{
  unsigned int flags = IOW_WAIT_SIGNAL;
  if (m->pending)
    {
      if (TUN_OUT (&m->pending->context))
	flags |= IOW_TO_TUN;
      if (LINK_OUT (&m->pending->context))
	flags |= IOW_TO_LINK;
    }
  else if (mbuf_defined (m->mbuf))
    flags |= IOW_MBUF;
  else
    flags |= IOW_READ;

  return flags;
}


/**************************************************************************/
/**
 * Main event loop for OpenVPN in UDP server mode.
 * @ingroup eventloop
 *
 * This function implements OpenVPN's main event loop for UDP server mode.
 *  At this time, OpenVPN does not yet support multithreading.  This
 * function's name is therefore slightly misleading.
 *
 * @param top - Top-level context structure.
 */
static void
tunnel_server_udp_single_threaded (struct context *top)
{
  struct multi_context multi;

  top->mode = CM_TOP;
  context_clear_2 (top);

  /* initialize top-tunnel instance */
  init_instance_handle_signals (top, top->es, CC_HARD_USR1_TO_HUP);
  if (IS_SIG (top))
    return;
  
  /* initialize global multi_context object */
  multi_init (&multi, top, false, MC_SINGLE_THREADED);

  /* initialize our cloned top object */
  multi_top_init (&multi, top, true);

  /* initialize management interface */
  init_management_callback_multi (&multi);

  /* finished with initialization */
  initialization_sequence_completed (top, ISC_SERVER); /* --mode server --proto udp */

  /* per-packet event loop */
  while (true)
    {
      perf_push (PERF_EVENT_LOOP);

      /* set up and do the io_wait() */
      multi_get_timeout (&multi, &multi.top.c2.timeval);
      io_wait (&multi.top, p2mp_iow_flags (&multi));
      MULTI_CHECK_SIG (&multi);

      /* check on status of coarse timers */
      multi_process_per_second_timers (&multi);

      /* timeout? */
      if (multi.top.c2.event_set_status == ES_TIMEOUT)
	{
	  multi_process_timeout (&multi, MPP_PRE_SELECT|MPP_CLOSE_ON_SIGNAL);
	}
      else
	{
	  /* process I/O */
	  multi_process_io_udp (&multi);
	  MULTI_CHECK_SIG (&multi);
	}
      
      perf_pop ();
    }

  /* shut down management interface */
  uninit_management_callback_multi (&multi);

  /* save ifconfig-pool */
  multi_ifconfig_pool_persist (&multi, true);

  /* tear down tunnel instance (unless --persist-tun) */
  multi_uninit (&multi);
  multi_top_free (&multi);
  close_instance (top);
}

void
tunnel_server_udp (struct context *top)
{
  tunnel_server_udp_single_threaded (top);
}

#endif
