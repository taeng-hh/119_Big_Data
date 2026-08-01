package androidtown.org.a119_big_data;

public class HospitalPlace {
    public String name;
    public String department;
    public String address;
    public String gu;
    public double latitude;
    public double longitude;
    public String tel;

    public HospitalPlace() {}

    public HospitalPlace(String name, String department, String address, String gu, double latitude, double longitude, String tel){
        this.name = name;
        this.department = department;
        this.address = address;
        this.gu = gu;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tel = tel;
    }
}
