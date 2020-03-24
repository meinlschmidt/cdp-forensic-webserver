# cdp-forensic-webserver

This is a forensic webserver using a forked version of sauvegarde [continuous data protection for gnu/linux (cdpfgl)](https://github.com/dupgit/sauvegarde), 
which allows you the centralized capturing, restoring of files and directories at any given time and the forensic analysis of file changes with an entropy from clients in a network.

Feel free to contact us for improvements or questions.

## Architecture

The architecture consists of all the clients, from which you want to obtain data, the cdp-forensic-webserver and the [cdp-forensic-frontend](https://github.com/danieltrtwn/cdp-forensic-frontend), 
which provides the GUI. Every linux client is running a cdpfgl client which sends the data, whenever a file changes, to the cdpfgl server, 
which is running on the same linux machine as the cdp-forensic-webserver.

![](architecture.png?raw=true)

## Installation

Install the [forked version of cdpfgl](https://github.com/meinlschmidt/cdp-sauvegarde) by following the readme on every client and the server machine, which will be the cdp-forensic-webserver.
Gnu/linux operating systems that can run cdpfgl can be found in the readme, we used Ubuntu 18.04 LTS for clients and server.

Start the cdp server on the server machine using the default config with
```
sudo cdpfglserver
```

or create a config file for the cdp server or modify the existing one to your needs as described in the readme and run it on the server machine with
```
sudo cdpfglserver -c /path/to/config/file
```

Create a config file for every cdp client or modify the existing one to your needs as described in their readme.
The cdp client config file contains all monitored directories and the ip address of your server machine.
Run the cdp client on every client machine with
```
sudo cdpfglclient -c /path/to/config/file
```

Check out this repository on the server machine e.g. with IntelliJ and run the cdp-forensic-webserver.
If you did not use the default cdp server config, ensure that the value of cdpServerMetaDirectory in class WebserverApplication extends the file-directory path from your cdp server config by /meta.

Use the [cdp-forensic-frontend](https://github.com/danieltrtwn/cdp-forensic-frontend) as GUI to use the cdp-forensic-webserver services.
