#include<ESP8266WiFi.h> 
//const char* ssid = "Hotel Stalenergo"; //your WiFi Name
//const char* password = "8j47qGys";  //Your Wifi Password
const char* ssid = "RedmiNote4x"; //your WiFi Name
const char* password = "12345678";  //Your Wifi Password
int port = 8888;
int ledPin = 0; 
WiFiServer server(port);
void setup() {
  Serial.begin(115200);
  delay(10); 
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW); 
  Serial.println();
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid); 
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected"); 
  server.begin();
  Serial.println("Server started");
  Serial.print("Use this URL to connect: ");
  Serial.print("http://");
  Serial.print(WiFi.localIP());
  Serial.println("/"); 
}
void loop() {
  WiFiClient client = server.available();
  if (client) {
    Serial.println("new client");
  } else return;

  String request = "";
  int value = 1;
  while(client.connected()){
    while(client.available()>0){      //возвращает количество принятых вайтов, если данных нет возвращает 0
      request = client.readStringUntil('#');
    
      if (request.indexOf("ON") != -1)  {
        digitalWrite(ledPin, HIGH);
        value = 1;
      }
      if (request.indexOf("OFF") != -1)  {
        digitalWrite(ledPin, LOW);
        value = -1;
      }
    }
    //формирование синуса и посылка его клиенту
    for(unsigned int i=0; i<360; i++) {
      byte n = value*byte(sin(i*0.035)*127);
      client.write(n);
    }  
  }
  client.stop();
  Serial.println("Client disconnected");    
}
