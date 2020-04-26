package com.ludi.echo;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Server {

    public static final String LOCAL_HOST = "localhost";
    public static final int DEFAULT_PORT = 8888;
    AsynchronousServerSocketChannel asynchronousServerSocketChannel;

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void start() {
        try {
            asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
            // 绑定监听端口
            asynchronousServerSocketChannel.bind(new InetSocketAddress(LOCAL_HOST, DEFAULT_PORT));
            System.out.println("启动服务器，监听端口：" + DEFAULT_PORT);
            while (true) {
                // attachment任意辅助对象,不需要就不传
                asynchronousServerSocketChannel.accept(null, new AcceptHandler());
                // 防止主线程退出
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(asynchronousServerSocketChannel);
        }
    }
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object>{

        @Override
        public void completed(AsynchronousSocketChannel socketChannel, Object attachment) {
            if (asynchronousServerSocketChannel.isOpen()) {
                asynchronousServerSocketChannel.accept(null, this);
            }

            if (socketChannel != null && socketChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(socketChannel);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Map<String, Object> info = new HashMap<>();
                info.put("type", "read");
                info.put("buffer", buffer);
                socketChannel.read(buffer, info, handler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            // 处理错误的情况
        }
    }

    private class ClientHandler implements CompletionHandler<Integer, Map<String, Object>> {

        private AsynchronousSocketChannel socketChannel;

        public ClientHandler(AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }


        @Override
        public void completed(Integer result, Map<String, Object> attachment) {
            String type = (String) attachment.get("type");
            if ("read".equals(type)) {
                ByteBuffer buffer = (ByteBuffer) attachment.get("buffer");
                buffer.flip();
                attachment.put("type", "write");
                socketChannel.write(buffer, attachment, this);
                buffer.clear();
            }
            if ("write".equals(type)) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                attachment.put("type", "read");
                attachment.put("buffer", buffer);
                socketChannel.read(buffer, attachment, this);
            }
        }

        @Override
        public void failed(Throwable exc, Map<String, Object> attachment) {
            // 处理错误的情况
        }

    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

}


