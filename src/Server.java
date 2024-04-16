import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Server
{
    private static final String USERS_FILENAME = "users.txt";

    private final Object usersLock;

    private ServerSocket server;

    public Server()
    {
        initAdmins();
        usersLock = new Object();
    }

    /*
    Creates the users file the first time the server is run.
     */
    public void initAdmins()
    {
        if (new File(USERS_FILENAME).exists())
            return;
        List<User> users = new ArrayList<>();
        users.add(new Admin("admin", "12345"));
        saveUsers(users);
    }

    public void start()
    {
        try
        {
            System.out.println("Server listening.");
            server = new ServerSocket(8080);

            while (true)
            {
                Socket client = server.accept();

                Thread clientThread = new Thread(() ->
                {
                    System.out.println("Accepted client.");
                    Scanner sc = null;
                    PrintStream out = null;

                    try
                    {
                        sc = new Scanner(client.getInputStream());
                        out = new PrintStream(client.getOutputStream());
                        userMenu(sc, out);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        if (sc != null)
                            sc.close();
                        if (out != null)
                            out.close();
                    }
                });

                clientThread.start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // For offline testing
//        userMenu(new Scanner(System.in), System.out);
    }

    @SuppressWarnings("unchecked")
    public List<User> loadUsers()
    {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(USERS_FILENAME)))
        {
            return (List<User>) in.readObject();
        }
        catch (IOException e)
        {
            if (e instanceof InvalidClassException)
            {
                throw new RuntimeException("One or more of the User subclasses has likely changed." +
                        " Serializable versions are not supported." +
                        " Recreate the users file.", e);
            }

            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            // Should never happen
            throw new RuntimeException(e);
        }

        return null;
    }

    public void saveUsers(List<User> users)
    {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(USERS_FILENAME)))
        {
            out.writeObject(users);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void registerUser(String userName, String password, UserType userType) throws CredentialsException
    {
        User user  = UserFactory.createUser(userName, password, userType);
        synchronized (usersLock)
        {
            List<User> users = loadUsers();
            users.add(user);
            saveUsers(users);
        }
    }

    private User login(String userName, String password)
    {
        synchronized (usersLock)
        {
            for (User user : loadUsers())
            {
                if (Objects.equals(user.getUserName(), userName) && Objects.equals(user.getPassword(), password))
                    return user;
            }

            return null;
        }
    }

    private void userMenu(Scanner sc, PrintStream out)
    {
        while (true)
        {
            out.println("Login? Y/N");
            String login = sc.nextLine();

            if (!login.equalsIgnoreCase("Y"))
            {
                out.println("Goodbye.");
                return;
            }

            out.println("Enter username:");
            String userName = sc.nextLine();

            out.println("Enter password:");
            String password = sc.nextLine();

            User user = login(userName, password);

            if (user == null)
            {
                out.println("Error: Invalid login.");
                continue;
            }

            switch (user.getUserType())
            {
                case ADMIN:
                {
                    adminMenu(sc, out, (Admin) user);
                    break;
                }
                case CUSTOMER:
                {
                    customerMenu(sc, out, (Customer) user);
                    break;
                }
                case EMPLOYEE:
                {
                    employeeMenu(sc, out, (Employee) user);
                    break;
                }
            }
        }
    }

    private void adminMenu(Scanner sc, PrintStream out, Admin admin)
    {
        out.println("Logged in as admin.");

        out.println("Enter user type to create: (ADMIN | CUSTOMER | EMPLOYEE");
        try
        {
            UserType userType = UserType.valueOf(sc.nextLine().toUpperCase());

            out.println("Enter username:");
            String userName = sc.nextLine();

            out.println("Enter password:");
            String password = sc.nextLine();

            registerUser(userName, password, userType);

            out.println("Success.");
        }
        catch (IllegalArgumentException e)
        {
            out.println("Error: Invalid user type.");
        }
        catch (CredentialsException e)
        {
            out.println(e.getMessage());
        }
    }

    private void customerMenu(Scanner sc, PrintStream out, Customer customer)
    {
        out.println("Logged in as a Customer.");


    }

    private void employeeMenu(Scanner sc, PrintStream out, Employee employee) {
        out.println("Logged in an Employee");


    }
}
