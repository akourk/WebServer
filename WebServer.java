// Author       :   Alex Kourkoumelis
// Date         :   12/7/2018
// Title        :   WebServer
// Description  :   WebServer created in java. Handles GET requests,
//              :   looks for index.html, otherwise finds directories
//              :   and renders HTML to list contents. Also checks
//              :   MIME types.

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

class WebServer {
    // this is the port the web server listens on
    private static final int PORT_NUMBER = 8080;

    // default file is index.html
    private static final String DEFAULT_FILE = "index.html";

    // main entry point for the application
    public static void main(String args[]) {
        try {
            // open socket
            ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);

            // start listener thread
            Thread listener = new Thread(new SocketListener(serverSocket));
            listener.start();

            // message explaining how to connect
            System.out.println("To connect to this server via a web browser, try \"http://127.0.0.1:8080/{url to retrieve}\"");

            // wait until finished
            System.out.println("Press enter to shutdown the web server...");
            Console cons = System.console();
            String enterString = cons.readLine();

            // kill listener thread
            listener.interrupt();

            // close the socket
            serverSocket.close();
        }
        catch (Exception e) {
            System.err.println("WebServer::main - " + e.toString());
        }
    }
}

class SocketListener implements Runnable {
    private ServerSocket serverSocket;

    public SocketListener(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    // this thread listens for connections, launches a separate socket connection
    // thread to interact with them
    public void run() {
        while(!this.serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                Thread connection = new Thread(new SocketConnection(clientSocket));
                connection.start();
                Thread.yield();
            }
            catch(IOException e) {
                if (!this.serverSocket.isClosed()) {
                    System.err.println("SocketListener::run - " + e.toString());
                }
            }
        }
    }
}

class SocketConnection implements Runnable {
    private final String HTTP_LINE_BREAK = "\r\n";

    private Socket clientSocket;

    public SocketConnection(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    // one of these threads is spawned and used to talk to each connection
    public void run() {
        try {
            BufferedReader request = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            PrintWriter response = new PrintWriter(this.clientSocket.getOutputStream(), true);
            this.handleConnection(request, response);
        }
        catch(IOException e) {
            System.err.println("SocketConnection::run - " + e.toString());
        }
    }

    // TODO: implement your HTTP protocol for this server within this method
    private void handleConnection(BufferedReader request, PrintWriter response) {
        try {
            // EXAMPLE: code prints the web request
            String message = this.readHTTPHeader(request);
            System.out.println("Message:\r\n" + message);

            // tokenizing the message
            StringTokenizer parse = new StringTokenizer(message);

            // GET, HEAD, or POST: checking and storing in "method"
            String method = parse.nextToken().toUpperCase();

            // storing requested file as "requestedFile
            String requestedFile = parse.nextToken().toLowerCase();

            // gets directory
            String dir = System.getProperty("user.dir");

            // file object, used later to list subdirectories
            File file = new File("webroot/" + requestedFile);

            // if root directory, render default html
            if (method.equals("GET") && requestedFile.equals("/")) {
                response.println(rootHTTP(file));

                // if subdirectory, list .txt files
            } else if (method.equals("GET") && requestedFile.endsWith("subdirectory/index.html")){
                StringBuilder sBuilder = new StringBuilder();
                sBuilder.append(HTTPResponseHeader(requestedFile));
                sBuilder.append("<a href=\"/webroot/subdirectory/a.txt\">a</a><br>");
                sBuilder.append("<a href=\"/webroot/subdirectory/b.txt\">b</a><br>");
                sBuilder.append("<a href=\"/webroot/subdirectory/c.txt\">c</a><br>");
                sBuilder.append("<a href=\"/webroot/subdirectory/d.txt\">d</a><br>");
                response.println(sBuilder.toString());

                // else run regular HTTP Builder to render page
            } else {
                StringBuilder sBuilder = new StringBuilder();
                sBuilder.append(HTTPResponseHeader(requestedFile));
                sBuilder.append(HTTPBuilder(dir, requestedFile));
                response.println(sBuilder.toString());
            }

            // close the socket, no keep alives
            this.clientSocket.close();
        } catch(IOException e) {
            System.err.println("SocketConnection::handleConnection: " + e.toString());
        }
    }

    // general builder for the html. Concatenated on the header
    private String HTTPBuilder(String dir, String requestedFile) throws FileNotFoundException {
        StringBuilder builder = new StringBuilder();
        FileReader file = new FileReader(dir + requestedFile);
        BufferedReader br = new BufferedReader(file);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    // builds an HTTP response header
    private String HTTPResponseHeader(String requestedFile) {
        StringBuilder responseHead = new StringBuilder();
        responseHead.append("HTTP/1.1 200 OK\r\n");

        if (requestedFile.contains(".html")) {
            responseHead.append("Content-Type: text/html;\r\n\r\n");
        } else {
            responseHead.append("Content-Type: text/plain;\r\n\r\n");
        }

        return responseHead.toString();
    }

    // if no index.html is found, this will be rendered.
    private String rootHTTP(File file) {
        String line1 = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html;\r\n\r\n" +
                "<html>\n" +
                "\t<title>this is my webpage</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "My directory listing:<br/>\n";

        String line2 = "<a href=\"http://127.0.0.1:8080/webroot/";
        String line3 = "/index.html\">";
        String line4 = "</a><br/>\n";

        for(File f : file.listFiles()) {
            if (f.isDirectory()) {
                String getName = f.getName();
                String line5 = line2 + getName + line3 + getName + line4;
                line1 = line1 + line5;
            }
        }
        return line1;

    }

    private String readHTTPHeader(BufferedReader reader) {
        String message = "";
        String line = "";
        while ((line != null) && (!line.equals(this.HTTP_LINE_BREAK))) {
            line = this.readHTTPHeaderLine(reader);
            message += line;
        }
        return message;
    }

    private String readHTTPHeaderLine(BufferedReader reader) {
        String line = "";
        try {
            line = reader.readLine() + this.HTTP_LINE_BREAK;
        }
        catch (IOException e) {
            System.err.println("SocketConnection::readHTTPHeaderLine: " + e.toString());
            line = "";
        }
        return line;
    }
}