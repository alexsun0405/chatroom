package com.ludi.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ChatClient {
    public static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    public static final int DEFAULT_SERVER_PORT = 8888;
    public static final String QUIT = "quit";
    public static final int BUFFER = 1024;
    private String host;
    private int port;
    private SocketChannel socketChannel;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Selector selector;
    private Charset charset = StandardCharsets.UTF_8;

    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
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

    private void start() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(host, port));

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    handles(selectionKey);
                }
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClosedSelectorException e) {
            // 不处理该异常
        } finally {
            close(selector);
        }
    }

    private void handles(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        // CONNECT事件 - 连接就绪
        if (selectionKey.isConnectable()) {
            // isConnectionPending 如果返回true，表示连接已经成功，需要我们再调用finishConnect方法。
            // 如果返回false，表示连接未被接受
            if (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
                // 处理用户输入
                new Thread(new UserInputHandler(this)).start();
            }
            // 注册channel
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
        // READ事件 - 服务器转发消息
        else if (selectionKey.isReadable()) {
            String msg = receive(socketChannel);
            if (msg.isEmpty()) {
                // 服务器异常
                close(selector);
            } else {
                System.out.println(msg);
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer) > 0) {
            rBuffer.flip();
        }
        return String.valueOf(charset.decode(rBuffer));
    }

    public void send(String input) throws IOException {
        if (input.isEmpty()) {
            return;
        }
        wBuffer.clear();
        wBuffer.put(charset.encode(input));
        wBuffer.flip();
        while (wBuffer.hasRemaining()) {
            socketChannel.write(wBuffer);
        }

        // 检查用户是否退出
        if (readyToQuit(input)) {
            close(selector);
            System.out.println("用户退出...");
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient("127.0.0.1", 7777);
        chatClient.start();
    }


}
