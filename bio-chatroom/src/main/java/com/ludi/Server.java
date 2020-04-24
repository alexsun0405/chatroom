package com.ludi;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 简单的Server端实现
 */
public class Server {
    public static void main(String[] args) {
        final String QUIT = "quit";
        final int DEFAULT_PORT = 8888;
        ServerSocket serverSocket = null;

        try {
            //1 绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口：" + DEFAULT_PORT);

            while (true) {
                //2 等待客户端连接
                Socket socket = serverSocket.accept();
                System.out.println("客户端【" + socket.getPort() + "】已经连接");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                //3 读取客户端发来的消息，如果客户端关闭了，那么返回 null
                String msg = null;
                while ((msg = reader.readLine()) != null) {
                    System.out.println("客户端【" + socket.getPort() + "】" + msg);
                    writer.write("服务器" + msg + "\n");
                    // 发送缓冲区数据
                    writer.flush();
                    // 查看客户端是否退出
                    if (QUIT.equals(msg)) {
                        System.out.println("客户端【" + socket.getPort() + "】已断开");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    System.out.println("关闭serverSocket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
