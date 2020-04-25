package com.ludi.my;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Acceptor {
    public static void main(String[] args) {
        final int DEFAULT_PORT = 8888;
        ServerSocket serverSocket = null;
        List<Socket> sockets = new ArrayList<>();
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("等待客户端连接......");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("客户端【" + socket.getPort() + "】已连接");
                sockets.add(socket);
                new Thread(new SocketHandler(socket,sockets)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class SocketHandler implements Runnable{

    private Socket socket;
    private List<Socket> sockets;

    public SocketHandler(Socket socket, List<Socket> sockets) {
        this.socket = socket;
        this.sockets = sockets;
    }

    @Override
    public void run() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                System.out.println("客户端【" + socket.getPort() + "】" + msg);
                for (Socket other : sockets) {
                    if (other == this.socket)
                        continue;
                    System.out.println("发送给其他socket");
                    BufferedWriter otherWriter = new BufferedWriter(new OutputStreamWriter(other.getOutputStream()));
                    otherWriter.write("客户端【" + other.getPort() + "】" + msg + "\n");
                    otherWriter.flush();
                    System.out.println("已发送");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
