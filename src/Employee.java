public class Employee extends User {
    public Employee(String username,String password){
        super(username,password);
    }
    @Override
    public UserType getUserType(){
        return UserType.EMPLOYEE;
    }
}
