package cn.edu.bupt.niclab.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import cn.edu.bupt.niclab.event.BaseEvent;
import cn.edu.bupt.niclab.event.FileDownloadEvent;
import de.greenrobot.event.EventBus;

/**
 * Created by zhengmeng on 2015/1/19.
 */
public class FileTransfer {
    
    
    public static void downloadFile(String fileUrl, String localPath){

        try {
            URL url = new URL(fileUrl);
            //create the new connection
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            //set up some things on the connection
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);

            //and connect!
            urlConnection.connect();

            //create a new file, specifying the path, and the filename
            //which we want to save the file as.
            File file = new File(localPath);
            if (file.exists()){
                file.delete();
            }
            FileOutputStream fileOutput = new FileOutputStream(file);
            InputStream inputStream = urlConnection.getInputStream();
            //this is the total size of the file
            int totalSize = urlConnection.getContentLength();
            //variable to store total downloaded bytes
            int downloadedSize = 0;

            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0; //used to store a temporary size of the buffer
            //now, read through the input buffer and write the contents to the file
            FileDownloadEvent event = new FileDownloadEvent(FileDownloadEvent.EVENT_PROGRESS);
            event.setDownloadedSize(downloadedSize);
            event.setDownloadedSize(totalSize);
            EventBus.getDefault().post(event);
            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                //add the data in the buffer to the file in the file output stream (the file on the sd card
                fileOutput.write(buffer, 0, bufferLength);
                //add up the size so we know how much is downloaded
                downloadedSize += bufferLength;
                //updateProgress(downloadedSize, totalSize);
                event.setDownloadedSize(downloadedSize);
                EventBus.getDefault().post(event);
            }
            //close the output stream when done
            fileOutput.close();
            //
            event.setResult(BaseEvent.EVENT_SUCCESS);
            EventBus.getDefault().post(event);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
