import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    public static void main(String[] args) throws IOException {
        Scanner sc=new Scanner(System.in);
        System.out.println("请输入您所需要的服务u/d");
        String command=sc.next();
        HashMap<String,Integer> path_id =new HashMap<>();//文件名与id对应关系
        ArrayList<ConnectServer>ConnectServerList=new ArrayList<>();//存储有哪些连接服务器的线程

        if(command.equals("u")){
            System.out.println("请输入您想要上传的文件地址，并在输入完后输入<");
            String path;
            ArrayList<String>filePaths=new ArrayList<>();
            while(!(path=sc.next()).equals("<")){
                checkDir(path,filePaths);
            }
            ExecutorService executorService=Executors.newFixedThreadPool(filePaths.size());
            for(int i=0;i<filePaths.size();i++){
                path_id.put(filePaths.get(i),i);
                ConnectServer connectServer=new ConnectServer("localhost",8000,filePaths.get(i),"u");
                ConnectServerList.add(connectServer);
                executorService.execute(connectServer);
            }

            UserActions(sc, ConnectServerList, filePaths, executorService);


        }else if(command.equals("d")){
            System.out.println("请输入您想要上传的文件名称，并在输入完后输入<");
            String path;
            ArrayList<String>fileName=new ArrayList<>();
            while(!(path=sc.next()).equals("<")){
                fileName.add(path);
            }
            ExecutorService executorService=Executors.newFixedThreadPool(fileName.size());
            for(int i=0;i<fileName.size();i++){
                path_id.put(fileName.get(i),i);
                ConnectServer connectServer=new ConnectServer("localhost",8000,fileName.get(i),"d");
                ConnectServerList.add(connectServer);
                executorService.execute(connectServer);
            }

            UserActions(sc,ConnectServerList, fileName, executorService);


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

    static void reportProgress(ArrayList<ConnectServer>ConnectServerList){
        for(int i=0;i<ConnectServerList.size();i++){
            if(ConnectServerList.get(i).getStatus()!=Status.Finish&&ConnectServerList.get(i).getStatus()!=Status.Delete){
                ConnectServerList.get(i).setStatus(Status.Report);
            }
        }
    }

    static void UserActions(Scanner sc, ArrayList<ConnectServer> connectServerList, ArrayList<String> filePaths, ExecutorService executorService) {
        String command;
        StatusMonitor:while(true){
            System.out.println("检查执行状态请输入c");
            command=sc.next();
            int cnt=0;
            for(int i=0;i<filePaths.size();i++){
                if(connectServerList.get(i).getStatus()==Status.Finish|| connectServerList.get(i).getStatus()==Status.Delete)cnt++;
                if(cnt==filePaths.size())break StatusMonitor;
            }
            if(command.equals("c")){
                reportProgress(connectServerList);
            }
        }
        executorService.shutdown();
    }

}
class ConnectServer implements Runnable{
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final String aimPath;
    private final String command;
//    private volatile int status=-1;//0正在进行,1报告一次进度,2暂停,3已完成,4终止任务
    private volatile Status status;

    public ConnectServer(String host,int port, String aimPath,String command) throws IOException {
        socket=new Socket(host,port);
        this.aimPath=aimPath;
        this.command=command;
        this.in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out=new PrintWriter(socket.getOutputStream(),true);

    }

    public void setStatus(Status status) {
        this.status = status;
    }
    public Status getStatus(){
        return this.status;
    }

    @Override
    public void run() {
        try {
            status=Status.Transfom;
            if (command.equals("u")) {
                upload();
            }else if(command.equals("d")){
                download();
            }

            in.close();
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
    void upload() throws IOException, InterruptedException {
        out.println("u");
        out.println(aimPath);
        String infoFromServer=in.readLine();
        if(infoFromServer.equals("__###ready###__")){
            File file=new File(aimPath);
            BufferedReader bufferedReader=new BufferedReader(new FileReader(file));
            //首先统计文本内的byte数目并告知服务端
            long byteCount= Toolbox.countBytes(file);
            out.println(byteCount);

            String line;
            while(true){
                if(status==Status.Transfom) {
                    Thread.sleep(200);
                    line = bufferedReader.readLine();
                    if(line!=null) {
                        out.println(line);
                    }else {
                        status=Status.Finish;
                        out.println("__###finish###__");
                        System.out.println(aimPath+"文件已上传完成");
                        break;
                    }
                }else if(status==Status.Report){
                    out.println("__###check###__");
                    System.out.println(in.readLine());
                    status=Status.Transfom;
                }
            }

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
            long bytesAmount=Long.parseLong(in.readLine());
            long bytesCount=0;
            File file=new File("localStorage/"+aimPath);
            BufferedWriter writer=new BufferedWriter(new FileWriter(file));

            while(true) {
                if(status==Status.Transfom){
                    infoFromServer= in.readLine();
                    if(!infoFromServer.equals("__###finish###__")){
                        writer.write(infoFromServer);
                        bytesCount+=infoFromServer.getBytes().length;
                    }else{
                        status=Status.Finish;
                        System.out.println(aimPath+"文件已接收完毕 ");
                        writer.close();
                        break;
                    }
                }else if(status==Status.Report){
                    System.out.println(Toolbox.calculateProgress(aimPath,bytesCount,bytesAmount));
                    status=Status.Transfom;
                }
            }
        }else if(infoFromServer.equals("__###None###__")){
            System.out.println("服务器上不存在目标文件");
        }else {
            System.out.println("接收到了意料之外的信息");
        }
    }



}