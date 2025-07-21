🧠 FITUR: Scraping Otomatis dari Sumber Awal

Mengisi basis data awal dari sumber terpercaya untuk keperluan analisa token secara internal

ATURAN: Sistem mengambil data saat inisialisasi

LATAR BELAKANG:
· JIKA aplikasi dijalankan untuk pertama kali· DAN belum ada database halal/haram token· DAN koneksi internet aktif· DAN sumber informasi terpercaya tersediaSKENARIO: Scraping awal ke sumber internal
· JIKA scraping engine aktifKEMUDIAN sistem mengakses situs berikut
https://www.cryptohalal.cc/currencies/4
https://sharlife.my/crypto-shariah/crypto/bitcoin
https://www.islamicfinanceguru.com/crypto
https://app.practicalislamicfinance.com/reports/crypto/


· MAKA sistem akan memindai semua halaman terkait

· DAN mengekstrak informasi: nama token, hukum, alasan hukum, referensi

· KEMUDIAN sistem menyimpannya ke dalam database lokal AI

· DAN menandainya sebagai sumber default internal
