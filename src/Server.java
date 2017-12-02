import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public class Server
{
    private static final int PORT = 9876;
    private static ServerSocket server;
    public static List<User> userList;

    public static void main(String[] args)
    {
        userList = new ArrayList<>();

        try
        {
            server = new ServerSocket(PORT);
            printToConsole("waiting for client...\n");
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        while(true)
        {
            Socket clientSocket = null;

            try
            {
                clientSocket = server.accept();
                printToConsole("Connection established.");
            } catch (IOException e)
            {
                e.printStackTrace();
                printToConsole("Client socket error");
                System.exit(1);
            }
            ConversationHandler conversationHandler = new ConversationHandler(clientSocket);
            conversationHandler.start();
//            imavHandler();
        }
    }

    public static void imavHandler()
    {
        Thread imavHandler = new Thread(() -> {
            while(true) {
                long currentTime = System.currentTimeMillis();
                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");  //for printing the time on the console
                Calendar cal = Calendar.getInstance();

                synchronized (userList)
                {
                    for (int i = 0; i < userList.size(); i++)
                    {
                        User user = userList.get(i);
                        if (user.getTime() + 300000 < currentTime)
                        {
                            System.out.println(dateFormat.format(cal.getTime()) + " Timeout for " + user.getName());
                            try
                            {
                                user.getSocket().close();
                                userList.remove(user);
                            } catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        imavHandler.start();
    }

    private static void printToConsole(String message)
    {
        System.out.println(">> " + message);
    }
}

class ConversationHandler extends Thread
{
    Socket socket;
    User user;
    BufferedWriter out;
    BufferedReader in;

    ConversationHandler(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public void run()
    {
        try
        {
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String userInput, command;
            int stopFlag = 0;
            do
            {
                userInput = in.readLine();
                printToConsole(userInput);
                command = userInput.substring(0,4).trim();

                switch (command)
                {
                    case "JOIN":            //JOIN <<user_name>>, <<server_ip>>:<<server_port>>
                        String userName = userInput.substring(5, userInput.indexOf(","));
                        boolean nameExists = checkUserName(userName);
                        boolean nameLegalFormat = checkUserNameValidation(userName);
                        if(!nameExists && nameLegalFormat)
                        {
                            user = new User(userName, this.socket);
                            addUserToList(user);
                            out.write("J_OK\n");
                            out.flush();
                            broadcastUserList();
//                            printToConsole(userName + " has just joined!\n");
                            printToConsole("user list now has " + Server.userList.size() + " users.\n");
                        }
                        if(nameExists||!nameLegalFormat)
                        {
                            out.write("J_ER\n");
                            out.flush();
                            this.socket.close();
                            printToConsole("Terminate connection to " + userName);
                        }
                        break;
                    case "DATA": broadcastUserInput(userInput);
                        break;
                    case "IMAV": receiveImavCommand(command);
                        break;
                    case "QUIT": cleanUpAfterUserQuit();
                        synchronized (Server.userList)
                        {
                            if(Server.userList.size() > 0)
                            {
                                broadcastUserList();
                            }
                        }
                        stopFlag = 1;
                }
            }while(stopFlag != 1);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static boolean checkUserNameValidation(String userName)
    {
        Pattern pattern = Pattern.compile("[A-Za-z0-9_-]+");
        return userName!= null && userName.length() <= 12 && pattern.matcher(userName).matches();
    }


    synchronized void addUserToList(User user)
    {
        Server.userList.add(user);
    }

    synchronized boolean checkUserName(String userName)
    {
        for(int i = 0; i < Server.userList.size(); i++)
        {
            if(userName.equals(Server.userList.get(i).getName()))
            {
                return true;
            }
        }
        return false;
    }

    //loop through user list to send message to everyone
    synchronized void broadcastUserInput(String userInput)
    {
//        String userInputContent = userInput.substring(5);
        String nameOfWhoSpeaks = user.getName();
        BufferedWriter broadcast;
        for(int i = 0; i < Server.userList.size(); i++)
        {
            User user = Server.userList.get(i);
//            if(!user.getName().equals(nameOfWhoSpeaks))
//            {
                try
                {
                    broadcast = new BufferedWriter(new OutputStreamWriter(user.getSocket().getOutputStream()));
                    broadcast.write(userInput + "\r\n");
                    broadcast.flush();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
//            }
        }
    }

    //LIST <<name1 name2 name3 ...>>
    synchronized void broadcastUserList()
    {
        BufferedWriter broadcast;

        String onlineUsers = getOnlineUserName();

        int userListSize = Server.userList.size();
        for(int i = 0; i < userListSize; i++)
        {
            User user = Server.userList.get(i);
            try
            {
                broadcast = new BufferedWriter(new OutputStreamWriter(user.getSocket().getOutputStream()));
                broadcast.write("LIST " + onlineUsers + "\n");
                broadcast.flush();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    synchronized String getOnlineUserName()
    {
        int userListSize = Server.userList.size();

        if(userListSize > 0)
        {
            String onlineUsers = Server.userList.get(0).getName();
            for(int i = 1; i < userListSize; i++)
            {
                User user = Server.userList.get(i);
                onlineUsers += " " + user.getName();
            }
            return onlineUsers;
        }
        return "";
    }

    synchronized void cleanUpAfterUserQuit()
    {
        try
        {
            out.write(">> Terminate connection to " + user.getName() + ".\n");
            out.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        for(int i = 0; i < Server.userList.size(); i++)
        {
            User userWantQuit = Server.userList.get(i);

            if(user.getName().equals(userWantQuit.getName()))
            {
                Server.userList.remove(userWantQuit);
            }
        }
        try
        {
            out.close();
            in.close();
        } catch (IOException e)
        {
            printToConsole("streams not closed probably");
            e.printStackTrace();
        }
    }

    //print user time
    synchronized void receiveImavCommand(String command)
    {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss"); //print time to console
        Calendar cal = Calendar.getInstance();

        user.setTime(cal.getTimeInMillis());
//        System.out.println(dateFormat.format(cal.getTime()) + ": " +  command + " from " + user.getName());
        System.out.println(user.getName() + " " + command + ": " + dateFormat.format(user.getTime())
                + " current time: " + dateFormat.format(cal.getTime()));
    }

    private static void printToConsole(String message)
    {
        System.out.println(">> " + message + "\r\n");
    }
}


class User
{
    String name;
    Socket socket;
    long time;

    User(String name, Socket socket)
    {
        this.name = name;
        this.socket = socket;
    }

    public String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    Socket getSocket()
    {
        return socket;
    }
}