/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package cn.edu.bupt.niclab;
import android.app.Application;

/*
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
*/

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import cn.edu.bupt.niclab.account.AccountManager;
import cn.edu.bupt.niclab.app.APPDataProvider;
import de.blinkt.openvpn.core.PRNGFixes;

/*
@ReportsCrashes(
        formKey = "",
        formUri = "http://reports.blinkt.de/report-icsopenvpn",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="report-icsopenvpn",
        formUriBasicAuthPassword="Tohd4neiF9Ai!!!!111eleven",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
)
*/
public class ICSOpenVPNApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        //init image loader
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration cfg = new ImageLoaderConfiguration.Builder(this)
                .defaultDisplayImageOptions(options)
                        //3MB Memory cache for image
                .memoryCache(new LruMemoryCache(1024 * 1024 * 3))
                        //unlimited disk cache for image
                .diskCache(new UnlimitedDiscCache(StorageUtils.getCacheDirectory(this)))
                .build();
        ImageLoader.getInstance().init(cfg);
        APPDataProvider.init(this);
        AccountManager.getManager().loadAccount(this);
        if (BuildConfig.DEBUG) {
            //ACRA.init(this);
        }
    }

}
