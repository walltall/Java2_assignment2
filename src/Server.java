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
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());

            // 获取输出流，用于向客户端发送数据
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            // 读取客户端发送的命令
            String command=in.readUTF();
            if(command.equals("u")){
                String aimPath=in.readUTF();
                upload(in,out,aimPath);
            }else if(command.equals("d")){
                String aimPath=in.readUTF();
                download(in, out,aimPath);
            }

            // 关闭连接
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void upload(DataInputStream in ,DataOutputStream out,String aimPath){
        //与客户端建立起联系，并准备开始下载文件
        try {
            File file = new File(aimPath);
            String localPath = "Storage/" + file.getName();
            File localFile = new File(localPath);
            if (localFile.createNewFile()) {
                out.writeUTF("__###ready###__");
                long bytesAmount =Long.parseLong(in.readUTF());
                long bytesCount=0;
                byte[]buffer=new byte[2048];
                BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(localFile));
                while (true){
                    String inputLine=in.readUTF();
                    if(inputLine.equals("__###finish###__")){
                        break;
                    }else if(inputLine.equals("__###check###__")) {
                        System.out.println(Toolbox.calculateProgress(aimPath, bytesCount, bytesAmount));
                        out.writeUTF(Toolbox.calculateProgress(aimPath, bytesCount, bytesAmount));
                    }else if(inputLine.equals("__###wait###__")) {
                        //此状态维持空体就行
                        continue;
                    }else if(inputLine.equals("__###delete###__")){
                        writer.close();
                        localFile.delete();
                        System.out.println(aimPath+"文件的传输已取消");
                        return;
                    }else if (inputLine.equals("__###content###__")){
                        int bytesRead=in.read(buffer);
                        bytesCount+=bytesRead;
                        if(bytesRead==-1){
                            break;
                        }
                        writer.write(buffer,0,bytesRead);
                    }
                }
                writer.close();
                System.out.println(aimPath+"文件已接收完毕");
            }else {
                out.writeUTF("__###exist###__");
                System.out.println(aimPath+"文件已存在于服务器中");
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    void download(DataInputStream in ,DataOutputStream out, String aimPath){
        try {
            File file=new File(aimPath);
            String localPath = "Storage/" + file.getName();
            File localFile = new File(localPath);
            if(localFile.exists()){
                out.writeUTF("__###exist###__");
                out.writeUTF(String.valueOf(Toolbox.countBytes(localFile)));//告知客户端将下载的文件的大小
                BufferedInputStream reader=new BufferedInputStream(new FileInputStream(localFile));
                byte[]buffer=new byte[2048];
                while(true){
                    String response=in.readUTF();
                    if(response.equals("__###accept###__")){
                        Thread.sleep(2);
                        int bytesRead=reader.read(buffer);
                        if(bytesRead!=-1) {
                            out.writeUTF("__###content###__");
                            out.write(buffer,0,bytesRead);
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
                out.writeUTF("__###finish###__");
                System.out.println(aimPath+"文件已传输完毕");
                reader.close();
            }else {
                out.writeUTF("__###None###__");
                System.out.println(aimPath+"文件不存在");
            }
        }catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}