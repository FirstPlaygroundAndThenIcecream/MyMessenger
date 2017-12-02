/*
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Server
{
    private static final int PORT = 8888;
    private static ServerSocket server;
    private static final List<ConversationHandler> conversationThreads = new ArrayList<>();
//    public static List<Socket> clientSockets;
//    public static ConcurrentHashMap<String, User> allUsers;

    public static void main(String[] args) throws Exception
    {
        try
        {
//            clientSockets = new ArrayList<>();
//            allUsers = new ConcurrentHashMap<>();

            System.out.println("waiting for client...");
            server = new ServerSocket(PORT);

        } catch (IOException e)
        {
            e.printStackTrace();
        }

//        UserHandler userHandler = new UserHandler();
//        List<User> clientList = userHandler.loadFromFile();

        while(true)
        {
            Socket clientSocket = server.accept();
            System.out.println("connection established");

            ConversationHandler conversationHandler = new ConversationHandler(clientSocket, conversationThreads);
            conversationHandler.start();
        }
    }
}

class ConversationHandler extends Thread
{
    Socket clientSocket;
    PrintWriter outputToClient;
    BufferedReader inputFromClient;
    List<ConversationHandler> conversationThreads;
//    UserHandler userHandler;
//    List<User> userArrayList;

    ConversationHandler(Socket clientSocket, List<ConversationHandler> conversationThreads)
    {
        this.clientSocket = clientSocket;
        this.conversationThreads = conversationThreads;
//        this.userHandler = new UserHandler();
    }

    @Override
    public void run()
    {
        try
        {
            this.outputToClient = new PrintWriter(clientSocket.getOutputStream());
            this.inputFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        userArrayList = userHandler.loadFromFile();
        System.out.println(">> user list has loaded: " + userArrayList.size() + " users in total");

        String userMsg = null;

        while(true)
        {
            try
            {
                userMsg = inputFromClient.readLine();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            String command = userMsg.substring(0, 4);

            switch (command)
            {
                case "JOIN":
                    //check user name unique then send feedback "ok" or error message
                    User user = generateUser(userMsg);
                    addUserToListAndSendFeedBackToClient(user);
                    break;

                case "DATA":
                    //find msg from who to whom then send to the receiver
                    broadcastClientMsg(userMsg.substring(5));
                    System.out.println(">> server broadcast " + userMsg);

                case "IMAV":
                    //alive message
                case "QUIT":
                    //usselist.getUser(name).closeSocket();
            }
        }
    }

    private User generateUser(String userMsg)
    {
        String userName = userMsg.substring(5, userMsg.indexOf(","));
        String ipAddress = userMsg.substring(userMsg.indexOf(",") + 2, userMsg.indexOf(":"));
        int port = Integer.parseInt(userMsg.substring(userMsg.indexOf(":")+1, userMsg.length()));

        User user = new User(userName, clientSocket);

        System.out.println(">> generate user: " + userName + ": " + ipAddress + ":" + port);

        return user;
    }

    private void addUserToListAndSendFeedBackToClient(User user)
    {
        if(userHandler.isExist(user.getName()))
        {
            outputToClient.println("J_ER");         //error messages should be more complete
            outputToClient.flush();

        }
        else
            outputToClient.println("J_OK");
            outputToClient.flush();
//            Server.clientSockets.add(clientSocket);
            userHandler.saveUserToFile(user);
            userHandler.addUser(userArrayList, user);
    }

    private synchronized void broadcastClientMsg(String clientMsg)
    {
        PrintWriter broadcast;
        for(int i = 0; i < userArrayList.size(); i++)
        {
            User user = userArrayList.get(i);
            Socket userSocket = user.getSocket();
            if( userSocket != null && user.isOnline())
            {
                try
                {
                    broadcast = new PrintWriter(userSocket.getOutputStream());
                    broadcast.println(clientMsg);
                    broadcast.flush();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}

class UserHandler
{
    List<User> userList;
    static final String FILENAME = "C:\\KEA\\Semester 03\\SWC\\MyMessenger\\src\\UserList";

    UserHandler()
    {
        userList = new ArrayList<>();
    }

    synchronized boolean addUser(List<User> userList, User user)
    {
        boolean addSuccessful;
        if(!isExist(user.getName()))
        {
            userList.add(user);
            addSuccessful = true;
        }else
            {
                addSuccessful = false;
            }
        return addSuccessful;
    }

    synchronized boolean isExist(String userName)
    {
        for(int i = 0; i < userList.size(); i++)
        {
            if(userName.equals(userList.get(i).getName()))
            {
                return true;
            }
        }
        return false;
    }

    synchronized void saveUserToFile(User user)
    {
        try
        {
            BufferedWriter writeToFile = new BufferedWriter
                    (new FileWriter(FILENAME, true));

            if(!isExist(user.getName().trim()))
            {
                writeToFile.write(user.userInfoForSaveToFile());
                writeToFile.flush();
            }
            writeToFile.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    synchronized void overrideFile()
    {
        try
        {
            BufferedWriter writeToFile = new BufferedWriter
                    (new FileWriter(FILENAME, true));

            for(User user: userList)
            {
                writeToFile.write(user.userInfoForSaveToFile());
                writeToFile.flush();
            }

            writeToFile.close();

        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    synchronized static List<User> loadFromFile()
    {
        List<User> userList = new ArrayList<>();
        try
        {
            BufferedReader readFromFile = new BufferedReader
                    (new FileReader(FILENAME));
            String userName;

            while((userName = readFromFile.readLine()) != null)
            {
                User user = new User(userName);
                userList.add(user);
                System.out.println(">> load from file: " + user.getName());

//                String[] userInfoArray = userInfo.split(":");
//                User user = new User(userInfoArray[0] + userInfoArray[1] + userInfoArray[2]);
//                userList.add(user);
            }
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return userList;
    }
}


class User
{
    String name;
    Socket socket;
    InetAddress inetAddress;
    int port;
    boolean online;

    User(String name, Socket socket)
    {
        this.name = name;
        this.socket = socket;
        if(socket != null)
            online = true;
    }

    User(String name)
    {
        this.name = name;
        socket = null;
    }

    String getName()
    {
        return name;
    }

    void setSocket(Socket socket)
    {
        this.socket = socket;
        if(socket != null)
        {
            online = true;
        }
    }

    boolean isOnline()
    {
        return online;
    }

    void setSocket(String ipAddress, int port)
    {
        try
        {
            inetAddress = InetAddress.getByName(ipAddress);
            this.port = port;
            this.socket = new Socket(inetAddress, port);
            if(socket != null)
                online = true;
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
          catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    Socket getSocket()
    {
        return socket;
    }

    void terminateConnection()
    {
        try
        {
            socket.close();
            online = false;
            inetAddress = InetAddress.getByName(null);
            port = 0;
            System.out.println(">> server has terminate the connection to the client");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    String userInfoForSaveToFile()
    {
        return name;
//        return name + ":" + socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort() + "\n";
    }
}
*/
