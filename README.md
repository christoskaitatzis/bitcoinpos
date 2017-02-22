
# Introduction
This is a simple Bitcoin Point of Sale application for android devices. It supports pre-set items and discounts.

It currently supports the following languages: English and Greek.

It currently supports the following currencies: EUR, USD, GBD, CNY, INR, JPY, RUB, ARS, AUD, BDT, BRL, CAD, XAF, XOF, DZD, CLP, COP, CZK, EGP, AED, GHS, HUF, IDR, IRR, ILS, KZT, KES, MYR, MXN, MAD, NZD, NGN, PKR, PEN, PHP, RON, SAR, ZAR, KRW, SDG, SEK, CHF, THB, TRY, UGX, UAH, UZS, VEF and VND.

If you are using another currency please contact me and I will add the currency required.


## Features
* Supports API version >= 16
* Supports BIP 21 URI scheme
* Uses Yahoo Finance API for conversion rates between currencies
* Uses Blockr API for now (could be easily extended to support other APIs, directly the Bitcoin network or both)

## Notes
* BTC to local currency conversion rate is used both ways. Local currency to BTC differs more so the amount in UI would be different between conversions
* Still due to decimal precision between conversions of the amount there will still be very slight precision variations!
* Requires client's wallet to support BIP 21 URI scheme (almost all do)

## Known Issues


## TODO    
* Clean code
* Add NFC support
