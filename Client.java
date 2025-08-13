import java.io.*;
import java.net.*;

public class client {
    Socket socket;
    BufferedReader br;
    PrintWriter out;
    public client(){
        try{
            System.out.println("Sending request to server ");
            socket=new Socket("127.0.0.1",8888);
            System.out.println("connection done");
            br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out=new PrintWriter(socket.getOutputStream());
            startReading();
            startwriting();
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
        public void startReading(){
            Runnable r1=()->{
                System.out.println("Reader started");
                while(true){
                    try{
                    String msg=br.readLine();
                    if(msg.equals("exit")){
                        System.out.println("server terminated the chat");
                        break;
                    }
                    System.out.println("server :"+msg);
                }catch(Exception e){
                    e.printStackTrace();

                }
                }
            };
            new Thread(r1).start();

        }
        public void startwriting(){
            Runnable r2=()->{
                System.out.println("writer  started");
                while(true){
                    try{
                        BufferedReader br1=new BufferedReader(new InputStreamReader(System.in));
                        String content=br1.readLine();
                        out.println(content);
                        out.flush();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }

            };
            new Thread(r2).start();

        }
    public static void main(String args[]){
        System.out.println("this is client");
        new client();
    }
}
