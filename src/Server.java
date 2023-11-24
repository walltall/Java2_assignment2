import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            // 创建服务器Socket，监听指定端口
            ServerSocket serverSocket = new ServerSocket(8000);
            System.out.println("服务器已启动，等待客户端连接...");

            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端已连接：" + clientSocket.getInetAddress().getHostAddress());

                // 创建一个新的线程来处理客户端请求
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            // 获取输入流，用于接收客户端发送的数据
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // 获取输出流，用于向客户端发送数据
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // 读取客户端发送的命令
            String command=in.readLine();
            if(command.equals("u")){
                String aimPath=in.readLine();
                upload(in,out,aimPath);
            }else if(command.equals("d")){
                String aimPath=in.readLine();
                download(in, out,aimPath);
            }else {

            }

            // 关闭连接
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void upload(BufferedReader in ,PrintWriter out,String aimPath){
        //与客户端建立起联系，并准备开始下载文件
        try {
            File file = new File(aimPath);
            String localPath = "Storage/" + file.getName();
            File localFile = new File(localPath);
            if (localFile.createNewFile()) {
                out.println("__###ready###__");
                long bytesAmount =Long.parseLong(in.readLine());
                long bytesCount=0;
                BufferedWriter writer = new BufferedWriter(new FileWriter(localFile));
                while (true){
                    String inputLine=in.readLine();
                    if(inputLine.equals("__###finish###__")){
                        break;
                    }else if(inputLine.equals("__###check###__")) {
                        System.out.println(Toolbox.calculateProgress(aimPath, bytesCount, bytesAmount));
                        out.println(Toolbox.calculateProgress(aimPath, bytesCount, bytesAmount));
                    }else if(inputLine.equals("__###wait###__")) {
                        //此状态维持空体就行
                        continue;
                    }else if(inputLine.equals("__###delete###__")){
                        writer.close();
                        localFile.delete();
                        System.out.println(aimPath+"文件的传输已取消");
                        return;
                    }else {
                        bytesCount+=inputLine.getBytes().length;
                        writer.write(inputLine + "\n");
                    }
                }
                writer.close();
                System.out.println(aimPath+"文件已接收完毕");
            }else {
                out.println("__###exist###__");
                System.out.println(aimPath+"文件已存在于服务器中");
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    void download(BufferedReader in ,PrintWriter out, String aimPath){
        try {
            File file=new File(aimPath);
            String localPath = "Storage/" + file.getName();
            File localFile = new File(localPath);
            if(localFile.exists()){
                out.println("__###exist###__");
                out.println(Toolbox.countBytes(localFile));//告知客户端将下载的文件的大小
                BufferedReader reader=new BufferedReader(new FileReader(localFile));
                while(true){
                    String response=in.readLine();
                    if(response.equals("__###accept###__")){
                        Thread.sleep(200);
                        String line=reader.readLine();
                        if(line!=null) {
                            out.println(line);
                        }else {
                            break;
                        }
                    }else if(response.equals("__###wait###__")){
                        continue;
                    }else if(response.equals("__###delete###__")){
                        reader.close();
                        System.out.println(aimPath+"文件的传输已取消");
                        return;
                    }
                }
                out.println("__###finish###__");
                System.out.println(aimPath+"文件已传输完毕");
                reader.close();
            }else {
                out.println("__###None###__");
                System.out.println(aimPath+"文件不存在");
            }
        }catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}