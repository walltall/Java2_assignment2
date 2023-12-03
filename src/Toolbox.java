import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Toolbox {
    static long countBytes(File aimFile) throws IOException {
        BufferedReader bufferedReader=new BufferedReader(new FileReader(aimFile));
        String temp;
        long byteCount=0;
        while((temp=bufferedReader.readLine())!=null){
            byteCount+=temp.getBytes().length;
        }
        bufferedReader.close();
        return byteCount;
    }
    static String calculateProgress(String aimPath, long bytesCount, long bytesAmount){
        return String.format("%s目前已传输: %f%% \n",aimPath,(double) 100*bytesCount/bytesAmount);
    }


    static void checkDir(String path, ArrayList<String> LocalFilesPath){
        File file=new File(path);
        if(file.exists()&&file.isDirectory()){
            File[]fileArray=file.listFiles();
            if (fileArray != null) {
                for (File value : fileArray) {
                    checkDir(value.getPath(), LocalFilesPath);
                }
            }
        }else {
            LocalFilesPath.add(path);
        }
    }
}
