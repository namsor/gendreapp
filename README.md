# GendRE Gender APP for Android

Predict gender from contact name. Gendre enriches Android contact titles (Mr., Ms., M.) with the gender inferred from a contact name.

![GenderApp](https://raw.githubusercontent.com/namsor/gendreapp/master/gendre_logo_v128.png)

Get GendRE App now on Google Play
https://play.google.com/store/apps/details?id=com.namsor.api.samples.gendreapp

Or here 
https://raw.githubusercontent.com/namsor/gendreapp/master/dist/Gendre_signed_v008_15.apk
On Opera
http://apps.opera.com/en_en/gender_app.html
http://apps.opera.com/fr_fr/gender_app.html
http://apps.opera.com/de_de/gender_app.html
http://apps.opera.com/pt_pt/gender_app.html
http://apps.opera.com/it_it/gender_app.html
http://apps.opera.com/ru_ru/gender_app.html

Get the latest dev version:
https://raw.githubusercontent.com/namsor/gendreapp/master/bin/Gendre.apk

We recommend backing up your contacts before running the program.

## Features
- NEW: cool Live Wallpaper with particle physics to show off your gender stats 
- Possibility to choose between three Title formats : Classic (Ms.,Mr.,M.), Gender (♀,♂,∅), Heart (♥,♤,♢), Chinese (女, 男) or Custom
- Creates groups to easily filter contacts (Male/Female)
- The gender prediction runs as a background service (every 10 sec, or 1 min, or 10 min or 1 hour)
- Your existing Title data (Mr., Dr. etc.) is not overwritten, unless you specifically request a wipe 
- Once all contacts are genderized, the App shows a summary of how many Female / Male contacts were detected
- You can share this #funstat on Twitter, G+, Facebook if you like

## Known issues
- Facebook contacts don't seem to be synchronized in Android the way other contacts are synchronized (LinkedIn, Google,...) so GendRE cannot enrich gender, but you can include Facebook contacts in the statistics
http://stackoverflow.com/questions/23195352/query-android-contacts-synchronized-from-facebook

## Languages and Localization
GendRE works with all international names (with very few exceptions). To help with translation/localization of the user interface (~300 words), please visit
https://www.transifex.com/projects/p/gendre-gender-app/
It helps if you can run the software once, to see the main messages in context. GendRE is currently available in: English, Arabic, Portuguese, Russian, Italian, French, German, Hebrew, Romanian, Hindi. 
We have issues with Bengali and Panjabi.   

## Physics Engine Credits
All credits for the particle physics go to Grant Kot http://grantkot.com/ and jgittins from XDADevelopers http://forum.xda-developers.com/showthread.php?t=878252

## Gender API
GenderApp uses the GendreAPI for name gender prediction. This is a free Application Programming Interface (API) to predict the gender of a personal name on a -1 (Male) to +1 (Female) scale, for a given geography/locale. You can test it directly from your web browser, entering this kind URL:
http://api.onomatic.com/onomastics/api/gendre/John/Smith/us
returns  -0.99 (ie. Male)  

![GenderApp](https://raw.githubusercontent.com/namsor/gendreapp/master/pics/gendre_1_en.png)
![GenderApp](https://raw.githubusercontent.com/namsor/gendreapp/master/pics/gendre_2_en.png)

## License

    Copyright 2014 NamSor Applied Onomastics

    Licensed under the GPLv2
    