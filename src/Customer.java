public class Customer extends User{
    public Customer(String username, String password){
        super(username,password);
    }
    @Override
    public UserType getUserType(){
        return UserType.CUSTOMER;
    }

}