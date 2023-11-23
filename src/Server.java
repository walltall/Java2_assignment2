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

            // 向客户端发送响应数据

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
                BufferedWriter writer = new BufferedWriter(new FileWriter(localFile));
                String inputLine;
                while (!(inputLine=in.readLine()).equals("__###finish###__")){
                    writer.write(inputLine+"\n");
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
                    BufferedReader reader=new BufferedReader(new FileReader(localFile));
                    String line;
                    while((line=reader.readLine())!=null){
                        out.println(line);
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
            }
        }
}