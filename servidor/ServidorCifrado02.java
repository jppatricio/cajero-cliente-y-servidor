import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.*;

public class ServidorCifrado02 {

  public static void main(String a[]) throws Exception {

    Logger logger = Logger.getLogger(ServidorCifrado02.class.toString());

    ServerSocket serverSocket = null;
    Socket socket = null;
    boolean estadoSesion = true;

    logger.log(Level.INFO, "Generando la llave...");
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(128);
    Key llave = keyGen.generateKey();
    logger.log(Level.INFO, "llave= {0}.", llave);
    logger.log(Level.INFO, "Llave generada!");

    ObjectOutput out = new ObjectOutputStream(new FileOutputStream("llave.ser"));
    out.writeObject(llave);
    out.close();

    try {
      logger.log(Level.INFO, "Escuchando por el puerto 8000");
      serverSocket = new ServerSocket(8000);
    } catch (IOException e) {
      logger.log(Level.INFO, "java.io.IOException generada");
      e.printStackTrace();
    }

    logger.log(Level.INFO, "Esperando a que los clientes se conecten...");
    while (estadoSesion) {
      try {
        socket = serverSocket.accept();
        logger.log(Level.INFO, "Se conecto un cliente: {0}.", socket.getInetAddress().getHostName());
        // Como ya hay socket, obtengo los flujos asociados a este

        // INIT de las variables globales dentro del while de interacción con el usuario
        boolean connectionIsNotLost = true;
        Cipher decipher = Cipher.getInstance("AES");
        Cipher cipher = Cipher.getInstance("AES");

        decipher.init(Cipher.DECRYPT_MODE, llave);
        cipher.init(Cipher.ENCRYPT_MODE, llave);

        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        String mensaje = "";
        String opcionLeida = "";
        String response = "";

        byte[] cipherText;
        int bytesLeidos;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (connectionIsNotLost) {

          // MANDO QUE SE ACEPTO Y LA BIENVENIDA JUNTO LAS OPCIONES DISPONIBLES
          mensaje = "Conexion aceptada \n Elige una de las siguientes opciones seguidas de un 'ENTER'\n\n 1.-\t Consultar saldo\n 2.-\t Depositar\n 3.-\t Retirar\n 4.-\t Salir";

          cipherText = cipher.doFinal(mensaje.getBytes());
          dos.write(cipherText.length);
          dos.write(cipherText);

          // LECTURA DE LA RESPUESTA AL CLIENTE
          bytesLeidos = dis.read();
          logger.log(Level.INFO, "bytes leidos= {0}.", bytesLeidos);
          byte arreglo[] = new byte[bytesLeidos];
          dis.read(arreglo);
          byte[] newPlainText = decipher.doFinal(arreglo);
          opcionLeida = new String(newPlainText, "UTF8");
          logger.log(Level.INFO, "El argumento DESENCRIPTADO es: {0}", opcionLeida);

          switch (opcionLeida) {
            case "1":
              cipherText = cipher.doFinal(getSaldo().getBytes());
              dos.write(cipherText.length);
              dos.write(cipherText);
              break;

            case "2":
              cipherText = cipher.doFinal("¿Cuanto quiere depositar?".getBytes());
              dos.write(cipherText.length);
              dos.write(cipherText);
              boolean correctDeposit = false;
              while (correctDeposit) {
                // LECTURA DE LA RESPUESTA AL CLIENTE
                bytesLeidos = dis.read();
                logger.log(Level.INFO, "bytes leidos= {0}.", bytesLeidos);
                dis.read(new byte[bytesLeidos]);
                newPlainText = decipher.doFinal(arreglo);
                opcionLeida = new String(newPlainText, "UTF8");
                logger.log(Level.INFO, "El argumento DESENCRIPTADO es: {0}", opcionLeida);
                try {
                  float f = Float.parseFloat(opcionLeida);
                  if (putDeposit(f)) {
                    cipherText = cipher.doFinal("Se ha registrado su deposito correctamente".getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctDeposit = true;
                  } else {
                    cipherText = cipher.doFinal("Algo salio mal. Reintente".getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctDeposit = true;
                  }
                } catch (Exception e) {
                  cipherText = cipher.doFinal("Error > Ingrese un valor numerico... *Ej=>200.50".getBytes());
                  dos.write(cipherText.length);
                  dos.write(cipherText);
                }
              }
              break;
            case "3":
              cipherText = cipher.doFinal("¿Cuanto quiere retirar?".getBytes());
              dos.write(cipherText.length);
              dos.write(cipherText);
              boolean correctWithdrawal = false;
              while (correctWithdrawal) {
                // LECTURA DE LA RESPUESTA AL CLIENTE
                bytesLeidos = dis.read();
                logger.log(Level.INFO, "bytes leidos= {0}.", bytesLeidos);
                dis.read(new byte[bytesLeidos]);
                newPlainText = decipher.doFinal(arreglo);
                opcionLeida = new String(newPlainText, "UTF8");
                logger.log(Level.INFO, "El argumento DESENCRIPTADO es: {0}", opcionLeida);
                try {
                  float f = Float.parseFloat(opcionLeida);
                  if (putWithdrawal(f).equalsIgnoreCase("OK")) {
                    cipherText = cipher
                        .doFinal(("Se ha registrado su retiro de $" + f + "MXN correctamente").getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctWithdrawal = true;
                  } else if (putWithdrawal(f).equalsIgnoreCase("ZERO")) {
                    cipherText = cipher.doFinal(("No tiene suficiente dinero...").getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctWithdrawal = true;
                  } else {
                    cipherText = cipher.doFinal(("Algo salio mal. Reintente").getBytes());
                    dos.write(cipherText.length);
                    dos.write(cipherText);
                    correctWithdrawal = true;
                  }
                } catch (Exception e) {
                  cipherText = cipher.doFinal("Error > Ingrese un valor numerico... *Ej=>200.50".getBytes());
                  dos.write(cipherText.length);
                  dos.write(cipherText);
                }
              }
              break;
            case "4":
              cipherText = cipher.doFinal("¡Hasta luego!".getBytes());
              dos.write(cipherText.length);
              dos.write(cipherText);
              connectionIsNotLost = false;
              break;

            default:
              break;
          }
        }

        dos.close();
        dis.close();
        socket.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "java.io.IOException generada");
        e.printStackTrace();
      }
      estadoSesion = false;
    }

  }

  public static void bytesToBits(byte[] texto) {
    StringBuilder stringToBits = new StringBuilder();
    for (int i = 0; i < texto.length; i++) {
      StringBuilder binary = new StringBuilder();
      byte b = texto[i];
      int val = b;
      for (int j = 0; j < 8; j++) {
        binary.append((val & 128) == 0 ? 0 : 1);
        val <<= 1;
      }
      System.out.println((char) b + " \t " + b + " \t " + binary);
      stringToBits.append(binary);
    }
    System.out.println("El mensaje completo en bits es:" + stringToBits);
  }

  public static String getSaldo() {
    try {
      FileReader reader = new FileReader("client.txt");
      BufferedReader bufferedReader = new BufferedReader(reader);
      String line = bufferedReader.readLine();
      reader.close();
      return ("Saldo es de: $" + line + " MXN");

    } catch (IOException e) {
      e.printStackTrace();
    }
    return 
  }

  public static Float getFloatSaldo() {
    try {
      FileReader reader = new FileReader("client.txt");
      BufferedReader bufferedReader = new BufferedReader(reader);
      String line = bufferedReader.readLine();
      reader.close();
      return Float.parseFloat(line);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static boolean putDeposit(float q) {
    try {
      FileWriter writer = new FileWriter("client.txt", false);
      Float s = getFloatSaldo();
      s = s + q;
      writer.write(s.toString());
      writer.close();
    } catch (IOException e) {
      return false;
    }
  }

  public static String putWithdrawal(float q) {
    try {
      FileWriter writer = new FileWriter("client.txt", false);
      Float s = getFloatSaldo();
      if ((s - q) < 0) {
        writer.close();
        return "Zero";
      }
      s = s - q;
      writer.write(s.toString());
      writer.close();
      return "OK";
    } catch (IOException e) {
      return "Error";
    }
  }

}