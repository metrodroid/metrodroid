# Taipei Metro (TRTC) station ID lists

`scrape_stationlist.py` is adapted from a shell script that appeared in the article [FuzzySecurity: EasyCard - Reverse Engineering an RFID payment system](http://www.fuzzysecurity.com/tutorials/rfid/4.html).

Data from the TRTC website:

* `stations_zh.csv`: https://web.metro.taipei/c/selectstation2010.asp
* `stations_en.csv`: https://web.metro.taipei/e/selectstation2010.asp

`scrape_stationlist.py` takes in these pages from stdin, and emits a CSV file of station IDs on stdout. A copy of the output is included in the repository for reference.

See also: [TRTC Open Data statement](https://english.metro.taipei/News_Content.aspx?n=784C655A49D3CD9D&sms=5E019B60E5224755&s=DCAD7B1493733FEC)
