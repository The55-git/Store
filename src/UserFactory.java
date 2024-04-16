import java.util.regex.Pattern;

public class UserFactory
{


    private final static Pattern emailPattern = Pattern.compile("[a-z]+@tu-sofia.bg");

    public static User createUser(String userName, String password, UserType userType) throws CredentialsException
    {
        switch (userType)
        {
            case ADMIN:
            {
                return new Admin(userName, password);
            }
            case CUSTOMER:
            {
                if (!emailPattern.matcher(userName).matches())
                    throw new CredentialsException("Error: Invalid email format.");
                if (password.length() < 5)
                    throw new CredentialsException("Error: Password must be at least 5 characters");

                return new Customer(userName, password);
            }
            case EMPLOYEE:
            {
                if (!emailPattern.matcher(userName).matches())
                    throw new CredentialsException("Error: Invalid email format.");
                if (password.length() < 5)
                    throw new CredentialsException("Error: Password must be at least 5 characters");
                return new Employee(userName, password);
            }
            default:
                return null;
        }
    }
}