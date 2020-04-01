# cdp-forensic-webserver

This is a forensic webserver being used as a submodule in [sauvegardeEX](https://github.com/LudwigEnglbrecht/sauvegardeEX).

Feel free to contact us for improvements or questions.

## Installation

This webserver requires a running [cdpfglserver](https://github.com/meinlschmidt/sauvegarde). Visit [sauvegardeEX](https://github.com/LudwigEnglbrecht/sauvegardeEX) for further information.

Install Java JDK 8 or higher.

Check out this repository, e.g. with IntelliJ, and run the cdp-forensic-webserver.

Update the variable "commandexternalcall" in /src/main/java/pseminar/cdp/webserver/Entropies.java to the external python script: [entropie](https://github.com/LudwigEnglbrecht/entropie).

Use the [cdp-forensic-frontend](https://github.com/danieltrtwn/cdp-forensic-frontend-private) as GUI to use the services provided by the cdp-forensic-webserver.
