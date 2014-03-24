# gendreapp

Predict gender from contact name. Gendre enriches Android contact titles (Mr., Ms., M.) with the gender inferred from a contact name.

![GenderApp](https://raw.githubusercontent.com/namsor/gendreapp/master/ic_launcher-web.png)

Get Gendre_v004 now:
https://raw.githubusercontent.com/namsor/gendreapp/master/dist/Gendre_v004.apk

Get the latest dev version:
https://raw.githubusercontent.com/namsor/gendreapp/master/bin/Gendre.apk

We recommend backing up your contacts before running the program.

## Features
- Possibility to choose between three Title formats : Classic (Ms.,Mr.,M.), Gender (♀,♂,∅), Heart (♥,♤,♢)
- The gender prediction runs as a background service (every 10 sec, or 1 min, or 10 min or 1 hour)
- Your existing Title data (Mr., Dr. etc.) is not overwritten, unless you specifically request a wipe 
- Once all contacts are genderized, the App shows a summary of how many Female / Male contacts were detected
- You can share this #funstat on Twitter, if you like

## Gender API
GenderApp uses the GendreAPI for name gender prediction. This is a free Application Programming Interface (API) to predict the gender of a personal name on a -1 (Male) to +1 (Female) scale, for a given geography/locale. You can test it directly from your web browser, entering this kind URL:
http://api.onomatic.com/onomastics/api/gendre/John/Smith/us
returns  -0.99 (ie. Male)  

![GenderApp](https://raw.githubusercontent.com/namsor/gendreapp/master/20140323_Gendre_pic1.png)
![GenderApp](https://raw.githubusercontent.com/namsor/gendreapp/master/20140323_Gendre_pic2.png)

## License

    Copyright 2014 NamSor Applied Onomastics

    Licensed under the GPLv2
    