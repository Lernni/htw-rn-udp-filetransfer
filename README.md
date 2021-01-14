Lenny Reitz
# Beleg Dateitransfer
>Diese Java-Konsolenanwendung ermöglicht den Austausch von Dateien zwischen zwei Hosts mittels UDP. Ein verlustfreier Datenverkehr wird über das Stop-And-Wait Protokoll gewährleistet. Das Programm entstand im Rahmen des [Rechnernetze Belegs](https://github.com/HTWDD-RN/Dateitransfer).
 
## Funktionen

### Konkrete Testszenarien
- [x] Funktion Client + Server ohne Fehlersimulation
- [x] Funktion Client + Server mit Fehlersimulation
- [ ] Funktion Client + Server über Hochschulproxy
- [x] Funktion Client + Hochschulserver ohne Fehlersimulation
- [x] Funktion Client + Hochschulserver mit Fehlersimulation

Anmerkung: Beide Hochschulserver empfangen die vom Client gesendete Datei korrekt und überprüfen auch die CRC der Datei mit Erfolg. Danach erwarten die Server jedoch weiterhin Datenpakete, obwohl die Übertragung eigentlich bereits abgeschlossen sein müsste. Die Server können erst wieder Daten empfangen, nachdem sie den Timeout durchlaufen haben. Mit der gesendeten Dateigröße kann dieses Problem nicht zusammenhängen, da die Server ja korrekt erkennen, wo die CRC auszulesen ist.

## Dokumentation
- [LaTeX Dokumentation](https://github.com/Lernni/htw-rn-udp-filetransfer/tree/main/doc/dokumentation.pdf)

## Installation
### 1. Kompilieren des Programms
`./make.sh`

### 2. Anwendung ausführen
#### 2.1 Server
`./filetransfer server <port> [<loss_rate> <avg_delay>] ['debug']`
- **port** - Integer-Wert (in der Regel über 1000) gibt an, auf welchem Port der Server auf Anfragen wartet
- **loss_rate** - (optional) Double-Wert zwischen 0.0 - 1.0 gibt an, mit welcher Wahrscheinlichkeit der Server ein verloren gegangenes Paket simuliert
- **avg_delay** - (optional) Integer-Wert in ms gibt an, mit welche mittlere Verzögerung der Server für Ankunft und Antwort von Paketen simuliert
- **'debug'** - (optional) Durch Angeben von 'Debug', wird der Server im Debug-Modus gestartet, es werden mehr Ausgaben in der Konsole angezeigt

#### 2.2 Client
`./filetransfer client <host_name/ip_address> <port> <file> ['debug']`
- **host_name/ip_address** - Adresse des Servers (Hostname oder IP), an die eine Datei gesendet werdem soll
- **port** - Integer-Wert (in der Regel über 1000) gibt an, an welchem Port der Server auf Dateien wartet
- **file** - Dateiname bzw. Dateipfad der Datei, die versendet werden soll
- **'debug'** - (optional) Durch Angeben von 'Debug', wird der Client im Debug-Modus gestartet, es werden mehr Ausgaben in der Konsole angezeigt
