package javatests;

/* Part of Issue2391 test */

public class DoubleHolder implements NumberHolder {

    private Double number = 0.0;

    @Override
    public Double getNumber() {
        return number;
    }

    public void setNumber(Double number) {
        this.number = number;
    }
}
