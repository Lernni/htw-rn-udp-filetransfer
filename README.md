Lenny Reitz
# Beleg Dateitransfer
>Diese Java-Anwendung ermöglicht den Austausch von Dateien zwischen zwei Hosts mittels UDP. Ein verlustfreier Datenverkehr wird über das Stop-And-Wait Protokoll gewährleistet. Das Programm entstand im Rahmen des [Rechnernetze Belegs](https://github.com/HTWDD-RN/Dateitransfer).
 
## Funktionen

### Konkrete Testszenarieren
- [x] Funktion Client + Server ohne Fehlersimulation
- [x] Funktion Client + Server mit Fehlersimulation
- [ ] Funktion Client + Server über Hochschulproxy
- [x] Funktion Client + Hochschulserver ohne Fehlersimulation
- [x] Funktion Client + Hochschulserver mit Fehlersimulation

## Dokumentation

## Installation
### 1. Kompilieren des Programms
`./make.sh`

### 2. Anwendung ausführen
#### 2.1 Server
`./filetransfer server <port> [<loss_rate> <avg_delay>] [debug]`
- **port** - Integer-Wert über 1000 gibt an, auf welcher Addresse der Server auf Anfragen wartet
- **loss_rate** - (optional) Double-Wert zwischen 0.0 - 1.0 gibt an, mit welcher Wahrscheinlichkeit der Server ein verloren gegangenes Paket simuliert
- **avg_delay** - (optional) Integer-Wert in ms gibt an, mit welche mittlere Verzögerung der Server für Ankunft und Antwort von Paketen simuliert
- **'debug'** - (optional) Durch Angeben von 'Debug', wird der Server im Debug-Modus gestartet, es werden mehr Ausgaben in der Konsole angezeigt

#### 2.2 Client
`./filetransfer client <host_name/ip_address> <port> <file> [debug]`
- **host_name/ip_address** - Adresse des Servers (Hostname oder IP), an die eine Datei gesendet werdem soll
- **port** - Integer-Wert über 1000 gibt an, an welchem Port der Server auf Dateien wartet
- **file** - Dateiname bzw. Dateipfad der Datei, die versendet werden soll
- **'debug'** - (optional) Durch Angeben von 'Debug', wird der Client im Debug-Modus gestartet, es werden mehr Ausgaben in der Konsole angezeigt