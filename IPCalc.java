/**
 * Author: Dmitry Larkin
 */
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QWidget;

public class IPCalc extends QWidget {
    int baseIPnumeric;
    int netmaskNumeric;
    int networkNumeric;
    int broadcastNumeric;
    int numberOfIPs;
    int numberOfBits;

    Ui_IPCalc ui = new Ui_IPCalc();

    /**
     * Конструктор класса
     */
    public IPCalc() {
        ui.setupUi(this);

        ui.iPAddressLineEdit.textChanged.connect(this, "validateIPAddress()");
        ui.prefixLenghtSpinBox.valueChanged.connect(this, "validateIPAddress()");

        updateMainWindow();
        show();
    }

    /**
     * Конструктор класса с указанием IP-адреса и префикса
     * @param IPinCIDRFormat IP-адрес в формате CIDR xx.xx.xx.xx/xx
     * @throws NumberFormatException
     */
    public IPCalc(String IPinCIDRFormat) throws NumberFormatException {
        String[] st = IPinCIDRFormat.split("\\/");
        if (st.length != 2)

            throw new NumberFormatException("Invalid CIDR format '"
                    + IPinCIDRFormat + "', should be: xx.xx.xx.xx/xx");

        String symbolicIP = st[0];
        String symbolicCIDR = st[1];

        Integer numericCIDR = new Integer(symbolicCIDR);
        if (numericCIDR > 32)

            throw new NumberFormatException("CIDR can not be greater than 32");

        /* IP */
        st = symbolicIP.split("\\.");

        if (st.length != 4)
            throw new NumberFormatException("Invalid IP address: " + symbolicIP);

        int i = 24;
        baseIPnumeric = 0;

        for (int n = 0; n < st.length; n++) {

            int value = Integer.parseInt(st[n]);

            if (value != (value & 0xff)) {

                throw new NumberFormatException("Invalid IP address: " + symbolicIP);
            }

            baseIPnumeric += value << i;
            i -= 8;
        }

        /* netmask from CIDR */
        if (numericCIDR < 8)
            throw new NumberFormatException("Netmask CIDR can not be less than 8");
        netmaskNumeric = 0xffffffff;
        netmaskNumeric = netmaskNumeric << (32 - numericCIDR);

        networkNumeric = baseIPnumeric & netmaskNumeric;

        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {

            if ((netmaskNumeric << numberOfBits) == 0)
                break;

        }

        for (int n = 0; n < (32 - numberOfBits); n++) {

            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;
        }

        broadcastNumeric = networkNumeric + numberOfIPs;

    }

	public static void main(String[] args) {
        QApplication.initialize(args);

        IPCalc ipcalc = new IPCalc();

        QApplication.exec();
	}

    /**
     * Обновление ярлыков
     */
    public void updateMainWindow(){
        String symbolicIP = ui.iPAddressLineEdit.text();
        int CIDR = ui.prefixLenghtSpinBox.value();
        IPCalc ip = new IPCalc(symbolicIP+"/"+CIDR);

        ui.addressRangeLineEdit.setText(ip.getHostAddressRange());
        ui.networkMaskLineEdit.setText(ip.getNetmask());
        ui.numberOfSubnetsLineEdit.setText(Long.toString(ip.getNumberOfSubnets()));
        ui.numberOfHostsLineEdit.setText(Long.toString(ip.getNumberOfHosts()));
        ui.networkAddressLineEdit.setText(ip.getNetworkAddress());
        ui.broadcastAddressLineEdit.setText(ip.getBroadcastAddress());
        ui.wildcardMaskLineEdit.setText(ip.getWildcardMask());
        ui.iPAddressLineEdit_2.setText(ip.getIPInBinary());
        ui.networkMaskLineEdit_2.setText(ip.getNetmaskInBinary());
        ui.networkAddressLineEdit_2.setText(ip.getNetworkAddressInBinary());
        ui.broadcastAddressLineEdit_2.setText(ip.getBroadcastInBinary());
     }

    /**
     * Строковое представление IP-адреса из числа
     * @param ip IP-адрес в виде числа
     * @return IP-адрес в виде строки
     */
    public String convertNumericIpToSymbolic(Integer ip) {
        StringBuffer sb = new StringBuffer(15);

        for (int shift = 24; shift > 0; shift -= 8) {

            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString((ip >>> shift) & 0xff));

            sb.append('.');
        }
        sb.append(Integer.toString(ip & 0xff));

        return sb.toString();
    }

    /**
     * Маска подсети (DDN)
     * @return Маска подсети
     */
    public String getNetmask() {
        StringBuffer sb = new StringBuffer(15);

        for (int shift = 24; shift > 0; shift -= 8) {

            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString((netmaskNumeric >>> shift) & 0xff));

            sb.append('.');
        }
        sb.append(Integer.toString(netmaskNumeric & 0xff));

        return sb.toString();
    }

    /**
     * Адресное пространство
     * @return Диапазон IP-адресов
     */
    public String getHostAddressRange() {

        Integer baseIP = baseIPnumeric & netmaskNumeric;
        String firstIP = convertNumericIpToSymbolic(baseIP + 1);
        String lastIP = convertNumericIpToSymbolic(baseIP + numberOfIPs - 1);
        return firstIP + " - " + lastIP;
    }

    /**
     * Количество хостов
     * @return Количество хостов
     */
    public Long getNumberOfHosts() {

        Double x = Math.pow(2, (32 - numberOfBits));

        if (x == -1)
            x = 1D;

        return x.longValue();
    }

    /**
     * Количество подсетей
     * @return Количество подсетей
     */
    public Long getNumberOfSubnets() {

        Double x = Math.pow(2, numberOfBits);

        if (x == -1)
            x = 1D;

        return x.longValue();
    }

    /**
     * Wildcard маска
     * @return Wildcard маска
     */
    public String getWildcardMask() {
        Integer wildcardMask = netmaskNumeric ^ 0xffffffff;

        StringBuffer sb = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {

            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString((wildcardMask >>> shift) & 0xff));

            sb.append('.');
        }
        sb.append(Integer.toString(wildcardMask & 0xff));

        return sb.toString();

    }

    /**
     * Адрес сети
     * @return Адрес сети
     */
    public String getNetworkAddress() {
        return convertNumericIpToSymbolic(baseIPnumeric & netmaskNumeric).toString();
    }

    /**
     * Широковещательный адрес
     * @return
     */
    public String getBroadcastAddress() {

        if (netmaskNumeric == 0xffffffff)
            return "0.0.0.0";

        String ip = convertNumericIpToSymbolic(broadcastNumeric);

        return ip;
    }

    /**
     * Двоичное представление IP-адреса из численного
     * @param number IP-адрес
     * @return IP-адрес
     */
    private String getBinary(Integer number) {
        String result = "";

        Integer ourMaskBitPattern = 1;
        for (int i = 1; i <= 32; i++) {

            if ((number & ourMaskBitPattern) != 0) {

                result = "1" + result; // the bit is 1
            } else { // the bit is 0

                result = "0" + result;
            }
            if ((i % 8) == 0 && i != 0 && i != 32)

                result = "." + result;
            ourMaskBitPattern = ourMaskBitPattern << 1;

        }
        return result;
    }

    /**
     * Двоичное представление IP-адреса
     * @return Возвращает IP-адрес в бинарном виде
     */
    public String getIPInBinary() {
        return getBinary(baseIPnumeric);
    }

    /**
     * Бинарный вывод маски подсте
     * @return Возвращает маску подсети в бинарном виде
     */
    public String getNetmaskInBinary() {

        return getBinary(netmaskNumeric);
    }

    /**
     * Двоичное представление широковещательного адреса
     * @return Возвращает широковещательный адрес в бинарном виде
     */
    public String getBroadcastInBinary() {
        return getBinary(broadcastNumeric);
    }

    /**
     * Двоичное представление адреса сети
     * @return Возвращает адрес сети в бинарном виде
     */
    public String getNetworkAddressInBinary() {
        return getBinary(networkNumeric);
    }

    /**
     * Проверка IP-адреса
     * Если true, то вызывается метод updateMainWindow;
     * @return true, если адрес допустим, и false, если недопустим
     */
    public boolean validateIPAddress() {

        String IPAddress = ui.iPAddressLineEdit.displayText();

        if (IPAddress.startsWith("0")) {
            return false;

        }

        if (IPAddress.isEmpty()) {

            return false;
        }

        if (IPAddress.matches("\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z")) {

            updateMainWindow();
            return true;
        }
        return false;
    }

}
