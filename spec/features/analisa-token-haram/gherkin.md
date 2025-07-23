🕌 FITUR: Analisa Token Haram
Membuat pengguna mengetahui informasi kehalalan sebuah token dari nama ticker atau alamat smart kontraknya

📌 ATURAN: Pengguna memasukkan nama ticker/token
LATAR BELAKANG:
· JIKA aplikasi dijalankan oleh user

· DAN terhubung dengan AI

· DAN pengguna login sebagai user

· DAN pengguna ingin mengecek kehalalan sebuah token


🔍 SKENARIO: Pengguna input dengan teks
· JIKA user ingin tahu kehalalan token $BTC

· KETIKA user input token $BTC

KEMUDIAN user mengetik prompt teks

 Prompt User:

 nginx
CopyEdit
Apakah token $BTC itu halal secara maqashid syariah?
· 
· DAN user mengetik enter

· MAKA AI menerima prompt dari user

· DAN akan mencari sumber informasi dari database awal dan user-provided (jika ada)

· DAN menyesuaikan jawaban berdasarkan fatwa yang tersedia

· DAN merancang jawaban berdasarkan kaidah syariah

· TETAPI tidak memberikan perbandingan dengan token atau komoditas lain

KEMUDIAN AI akan memberikan respon ke user

 Response AI:

 nginx
CopyEdit
Pengertian dari maqashid syariah adalah menjaga agama, jiwa, akal, keturunan, dan harta.
Token $BTC atau Bitcoin sendiri merupakan aset digital yang tidak mengandung riba secara langsung,
namun bisa mengandung unsur gharar tergantung pada penggunaannya.
Berdasarkan fatwa dari beberapa negara, seperti Majelis Ulama Indonesia, Dewan Syariah Bahrain, dll,
maka $BTC dapat dikategorikan halal selama tidak digunakan untuk transaksi yang haram.
· 
· DAN AI menyimpan histori chat pengguna

KEMUDIAN AI memberikan pertanyaan lanjutan

 Prompt AI:

 markdown
CopyEdit
Apakah kamu ingin lebih tahu tentang:
1. Fatwa negara lain tentang BTC?
2. Bagaimana hukum jual beli BTC dalam Islam?
· 
· KEMUDIAN user bisa menjawab

· DAN user bisa memilih untuk berhenti

· TETAPI AI akan otomatis berhenti jika tidak ada respon selama 5 menit



🎙️ SKENARIO: Pengguna input dengan audio
· JIKA user ingin tahu kehalalan token $SOL

· KETIKA user input token $SOL

KEMUDIAN user mengirimkan audio dengan berkata:

 Prompt User (Audio):

 bash
CopyEdit
$SOL itu halal gak sih?
· 
· MAKA sistem akan mengenali audio menjadi teks

· DAN AI akan menerima input tersebut seperti teks

· DAN akan memproses permintaan seperti skenario sebelumnya

KEMUDIAN AI memberikan respon teks dan (jika tersedia) balasan audio

 Response AI:

 nginx
CopyEdit
Solana atau $SOL merupakan platform blockchain. Tidak secara langsung mengandung unsur haram.
Namun harus dianalisis berdasarkan proyek yang dijalankan dalam ekosistemnya.
Untuk saat ini, $SOL belum dikategorikan haram oleh otoritas Islam resmi.
· 
· DAN AI menyimpan percakapan

DAN AI memberikan follow-up prompt seperti:

 Prompt AI:

 markdown
CopyEdit
Apakah kamu ingin melihat daftar DApps di ekosistem Solana yang tidak sesuai syariah?
1. Ya
2. Tidak



🕹️ SKENARIO: Pengguna memasukkan kontrak address
· JIKA user ingin mengecek token yang belum terkenal

· KETIKA user memasukkan smart contract address dari token ERC20 atau SPL

KEMUDIAN user mengetik:

 Prompt User:

 yaml
CopyEdit
Tolong analisa kontrak berikut: 0x1234567890abcdef...
· 
· MAKA AI mengambil metadata dari blockchain explorer (Solana/Ethereum)

· DAN mengidentifikasi jenis proyek, kategori bisnis, dan audit data

· DAN mengkategorikan berdasarkan prinsip maysir, gharar, riba, haram, dan batil

KEMUDIAN AI memberikan penilaian awal:

 Response AI:

 kotlin
CopyEdit
Token ini belum memiliki audit syariah formal.
Namun dari data yang kami kumpulkan, proyek ini terlibat dalam platform prediksi berbasis taruhan,
yang mengandung unsur maysir dan gharar.
· Maka token ini cenderung tidak sesuai dengan prinsip syariah.