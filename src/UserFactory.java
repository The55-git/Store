public class UserFactory {

    public static User createUser(String userName, String password, UserType userType) throws CredentialsException {
        switch (userType) {
            case ADMIN:
                return new Admin(userName, password);
            case CUSTOMER:
                validateCredentials(userName, password);
                return new Customer(userName, password);
            case EMPLOYEE:
                validateCredentials(userName, password);
                return new Employee(userName, password);
            default:
                return null;
        }
    }

    private static void validateCredentials(String userName, String password) throws CredentialsException {
        if (userName.length() < 3)
            throw new CredentialsException("Error: Username must be at least 3 characters.");
        if (password.length() < 5)
            throw new CredentialsException("Error: Password must be at least 5 characters.");
    }
}
