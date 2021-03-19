#include<ESP8266WiFi.h> 
const char* ssid = "Hotel Stalenergo"; //your WiFi Name
const char* password = "8j47qGys";  //Your Wifi Password
int ledPin = 0; 
WiFiServer server(80);
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
  if (!client) {
    return;
  }
  //Serial.println("new client");
  while(!client.available()){
    delay(1);
  } 
  String request = client.readStringUntil('\r');
  //Serial.println(request);
  client.flush(); 
  int value = 0;
  if (request.indexOf("/LED=ON") != -1)  {
    digitalWrite(ledPin, HIGH);
    value = 1;
  }
  if (request.indexOf("/LED=OFF") != -1)  {
    digitalWrite(ledPin, LOW);
    value = -1;
  }
  client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: text/html");
  client.println(""); 
  
  for(unsigned int i=0; i<360; i++) {
    byte n = value*byte(sin(i*0.035)*127);
    client.write(n);
  }  
     delay(1);
  //Serial.println("Client disonnected");
  //Serial.println("");   
}
