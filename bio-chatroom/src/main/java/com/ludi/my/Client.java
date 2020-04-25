package com.ludi.my;

import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final int DEFAULT_PORT = 8888;
        Socket socket = null;
        try {
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_PORT);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            new Thread(new MessageHandler(reader)).start();
            // 等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();
                // 发送消息给服务器
                writer.write(input + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

/**
 * 负责处理服务端发来的信息
 */
class MessageHandler implements Runnable {

    private BufferedReader reader;

    public MessageHandler(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("监听信息中...");
                String msg = reader.readLine();
                System.out.println(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}