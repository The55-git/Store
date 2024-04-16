import java.io.Serializable;

public class User implements Serializable {
    private String username;

    private String password;
    public User(String username, String password){
        this.username=username;
        this.password=password;
    }


    public UserType getUserType() {
        return null;
    }

    public String getUserName(){
        return username;
    }
    public void setUserName(){
        this.username=username;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(){
        this.password=password;
    }
}
