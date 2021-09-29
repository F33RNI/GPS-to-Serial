# GPS-to-Serial

![Icon](ICON_64.png "Icon")

[![GitHub license](https://img.shields.io/github/license/XxOinvizioNxX/GPS-to-Serial)](https://github.com/XxOinvizioNxX/GPS-to-Serial/blob/main/LICENSE)
[![Twitter](https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Ftwitter.com%2Ffern_hertz)](https://twitter.com/fern_hertz)

![Preview](PREVIEW.png "Preview")

## What is it for?

This application is currently used in [Liberty-Way project](https://github.com/XxOinvizioNxX/Liberty-Way)

You can create your own GPS robot without purchasing an additional GPS receiver 🙂

## Overview

Simple application for sending phone's GPS coordinates via USB serial port. A USB-UART converter is connected to the phone via USB-OTG. On the phone, you need to press the `CONNECT AND START` button, confirm the permissions **(after confirming the permissions, you need to press the button again)** and after that the phone will start receiving GPS coordinates with an interval of **1 second**. Latitude, longitude and accuracy are displayed as text on the layout. Each time new coordinates are received, they are packed into an **11 byte packet** and sent to the serial port at the baud rate you **specified before pressing the button**. To stop receiving GPS coordinates and close the serial port, press the `STOP` button.

## Serial packet structure
The packet consists of 11 bytes. At the beginning of the packet, 4 bytes are assigned to latitude, 4 to longitude. Then 1 byte of the XOR check-sum (XOR check-sum is calculated using 8 bytes of latitude and longitude), at the end 2 bytes of the packet ending: 0xEE and 0xEF.

**Latitude and longitude values are signed integers.** 6 decimal places (from degrees) are converted to 6 integer digits. (-90000000 to 90000000 for latitude and -180000000 to 180000000 for longitude). To get back the degrees of the float type, signed integers must be **divided by 1000000**.

**Latitude and longitude bytes are in Big-endian order.**

### Packet structure:
- byte 0-3: latitude bytes in big-endian order (-90000000 to 90000000)
- byte 4-7: longitude bytes in big-endian order (-180000000 to 180000000)
- byte 8: XOR check-sum of 0-7 bytes
- byte 9: 0xEE
- byte 10: 0xEF

### Serial packet example:

Serial packet example for latitude: `40.689249` and longitude: `-74.044500`

- Latitude: `40.689249` -> `40689249` -> `02 6C DE 61`
- Longitude: `-74.044500` -> `-74044500` -> `FB 96 2B AC`
- XOR check-sum: `02 XOR 6C XOR DE XOR 61 XOR FB XOR 96 XOR 2B XOR AC` -> `3B`
- Packet ending: `EE EF`

Result:
- (HEX) `02 6C DE 61 FB 96 2B AC 3B EE EF`
- (DEC) `2 108 222 97 251 150 43 172 59 238 239`
- (BIN) `00000010 01101100 11011110 01100001 11111011 10010110 00101011 10101100 00111011 11101110 11101111`

## Credits

The app uses the [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) library.
