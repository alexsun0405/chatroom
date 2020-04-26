package com.ludi.nio;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ChatServer {

    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    // 处理服务器端IO的通道
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    // 要从通道读取数据，即要把通道的数据写入buffer，所以这是一个可写的buffer
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    // 要向通道写入数据，即要把buffer的数据写入通道，所以这是一个可读的buffer
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    // 编码方式
    private Charset charset = StandardCharsets.UTF_8;

    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    private void start() {
        try {
            // 默认是阻塞是调用的模式
            serverSocketChannel = ServerSocketChannel.open();
            // 阻塞状态设置为false
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            // 获取selector
            selector = Selector.open();
            // 绑定selector监听的事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口：" + port + "...");

            while (true) {
                // 阻塞，直到事件发生
                selector.select();
                // 触发的事件集合
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    // 处理事件
                    handles(selectionKey);
                }
                // 清空处理过的事件集合
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭selector，会把注册到上面的通道关闭。
            close(selector);
        }
    }

    private void handles(SelectionKey selectionKey) throws IOException {
        // ACCEPT事件(ServerSocketChannel) - 客户端请求建立连接
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(socketChannel) + "已连接");
        }
        // READ事件(SocketChannel) - 客户端发送了消息
        else if (selectionKey.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            String fwdMsg = receive(socketChannel);
            if (fwdMsg.isEmpty()) {
                // 认为客户端发生了异常
                selectionKey.cancel();
                // 更新selector中事件的状态
                selector.wakeup();
            } else {
                forwardMessage(socketChannel, fwdMsg);
                // 检查用户是否退出
                if (readyToQuit(fwdMsg)) {
                    selectionKey.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(socketChannel) + "已断开");
                }
            }
        }
    }

    private void forwardMessage(SocketChannel socketChannel, String fwdMsg) throws IOException {
        // 找到目前在线的所有客户端,和selectedKeys的区别，keys方法获取的是所有注册的客户端，selectedKeys方法获取的是触发了事件的客户端
        Set<SelectionKey> selectionKeys = selector.keys();
        for (SelectionKey selectionKey : selectionKeys) {
            Channel channel = selectionKey.channel();
            if (channel instanceof ServerSocketChannel) {
                continue;
            }
            // channel 是有效的状态, 并且不是当前Client
            if (selectionKey.isValid() && !socketChannel.equals(channel)) {
                wBuffer.clear();
                wBuffer.put(charset.encode(getClientName(socketChannel) + fwdMsg));
                wBuffer.flip();
                while (wBuffer.hasRemaining()) {
                    ((SocketChannel) channel).write(wBuffer);
                }
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer) > 0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private String getClientName(SocketChannel client) {
        return "客户端【" + client.socket().getPort() + "】";
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }

}
