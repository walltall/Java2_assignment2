import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static void main(String[] args) throws IOException {
        Scanner sc=new Scanner(System.in);
        System.out.println("请输入您所需要的服务u/d");
        String command=sc.next();
        if(command.equals("u")){
            System.out.println("请输入您想要上传的文件地址，并在输入完后输入<");
            String path;
            ArrayList<String>filePaths=new ArrayList<>();
            while(!(path=sc.next()).equals("<")){
                checkDir(path,filePaths);
            }
            ExecutorService executorService=Executors.newFixedThreadPool(filePaths.size());
            for(int i=0;i<filePaths.size();i++){
                executorService.execute(new ConnectServer("localhost",8000,filePaths.get(i),"u"));
            }
            String nextCommand;

            executorService.shutdown();
        }else if(command.equals("d")){
            System.out.println("请输入您想要上传的文件名称，并在输入完后输入<");
            String path;
            ArrayList<String>fileName=new ArrayList<>();
            while(!(path=sc.next()).equals("<")){
                fileName.add(path);
            }
            ExecutorService executorService=Executors.newFixedThreadPool(fileName.size());
            for(int i=0;i<fileName.size();i++){
                executorService.execute(new ConnectServer("localhost",8000,fileName.get(i),"d"));
            }
        }else {
            System.out.println("输入有误");
        }
    }
    static void checkDir(String path,ArrayList<String>filesPath){
        File file=new File(path);
        if(file.exists()&&file.isDirectory()){
            File[]fileArray=file.listFiles();
            if (fileArray != null) {
                for (File value : fileArray) {
                    checkDir(value.getPath(), filesPath);
                }
            }
        }else {
            filesPath.add(path);
        }
    }
}
class ConnectServer implements Runnable{
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    String aimPath;
    String command;
    public ConnectServer(String host,int port, String aimPath,String command) throws IOException {
        socket=new Socket(host,port);
        this.aimPath=aimPath;
        this.command=command;
        this.in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out=new PrintWriter(socket.getOutputStream(),true);
    }

    @Override
    public void run() {
        try {
            if (command.equals("u")) {
                upload();
            }else if(command.equals("d")){
                download();
            }

            in.close();
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
    void upload() throws IOException {
        out.println("u");
        out.println(aimPath);
        String infoFromServer=in.readLine();
        if(infoFromServer.equals("__###ready###__")){
            File file=new File(aimPath);
            BufferedReader bufferedReader=new BufferedReader(new FileReader(file));
            String line;
            while((line =bufferedReader.readLine())!=null){
                out.println(line);
                line =bufferedReader.readLine();
            }
            out.println("__###finish###__");
            System.out.println(aimPath+"文件已上传完成");
        }else if(infoFromServer.equals("__###exist###__")){
            System.out.println(aimPath+"已经在服务器上存在了");
        }else {
            System.out.println("接收到了意料之外的信息");
        }

    }
    void download() throws IOException{
        out.println("d");
        out.println(aimPath);
        String infoFromServer=in.readLine();
        if(infoFromServer.equals("__###exist###__")){
            File file=new File("localStorage/"+aimPath);
            BufferedWriter writer=new BufferedWriter(new FileWriter(file));
            while(!(infoFromServer=in.readLine()).equals("__###finish###__")){
                writer.write(infoFromServer);
                writer.write("\n");
            }
            System.out.println(aimPath+"文件已接收完毕 ");
            writer.close();
        }else if(infoFromServer.equals("__###None###__")){
            System.out.println("服务器上不存在目标文件");
        }else {
            System.out.println("接收到了意料之外的信息");
        }
    }
}