package com.hhy.demo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleHttpServer {
	// 处理HttpRequest请求的线程池
	// 处理HttpRequest的线程池
    static ThreadPool<HttpRequestHandler> threadPool = new DefaultThreadPool<HttpRequestHandler>(1);
    // SimpleHttpServer的根路径
    static String basePath;
    static ServerSocket serverSocket;
    // 服务监听端口
    static int port = 8080;

    public static void setPort(int port) {
        if (port > 0) {
            SimpleHttpServer.port = port;
        }
    }

    public static void setBasePath(String basePath) {
        if (basePath != null && new File(basePath).exists() && new File(basePath).isDirectory()) {
            SimpleHttpServer.basePath = basePath;
        }
    }// 启动SimpleHttpServer

    public static void start() throws Exception {
        serverSocket = new ServerSocket(port);
        Socket socket = null;
        while ((socket = serverSocket.accept()) != null) {
            // 接收一个客户端Socket，生成一个HttpRequestHandler，放入线程池执行
            threadPool.execute(new HttpRequestHandler(socket));
        }
        serverSocket.close();
    }

	static class HttpRequestHandler implements Runnable {
		private Socket socket;

		public HttpRequestHandler(Socket socket) {
			super();
			this.socket = socket;
		}

		public void run() {
			String line = null;
			BufferedReader br = null;
			BufferedReader reader = null;
			PrintWriter out = null;
			InputStream in = null;

			try {
				reader = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String header = reader.readLine();
				// 由相对路径来计算绝对路径
				String filePath = basePath + header.split(" ")[1];
				out = new PrintWriter(socket.getOutputStream());
				// 对请求资源的后缀进行判断
				if (filePath.endsWith("jpg") || filePath.endsWith("ico")) {
					in = new FileInputStream(filePath);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					int i = 0;
					while ((i = in.read()) != -1) {
						baos.write(i);
					}
					byte[] array = baos.toByteArray();// 将流转化成字节数组
					out.println("Http/1.1 200 OK");
					out.println("Server:Molly");
					out.println("Context-Type:image/jpeg");
					out.println("");
					socket.getOutputStream().write(array, 0, array.length);
				} else {
					br = new BufferedReader(new InputStreamReader(
							new FileInputStream(filePath)));
					out = new PrintWriter(socket.getOutputStream());
					out.println("Http/1.1 200 OK");
					out.println("Server:Molly");
					out.println("Context-Type:text/html;charset=UTF-8");
					out.println("");
					while ((line = br.readLine()) != null) {
						out.println(line);
					}
				}
				out.flush();
			} catch (Exception e) {
				out.println("HTTP/1.1 500");
				out.println("");
				out.flush();
			} finally {
				try {
					close(br, in, reader, out, socket);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void close(BufferedReader br, InputStream in,
				BufferedReader reader, PrintWriter out, Socket socket2) throws Exception {
			br.close();
			in.close();
			reader.close();
			out.close();
			socket2.close();
		}
	}
}
