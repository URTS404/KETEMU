# Aturan ProGuard/R8 untuk Ketemu.
# Room, Hilt, dan supabase-kt (serialization) akan memerlukan aturan tambahan
# pada fase M3-M4; R8 sudah menyediakan consumer rules untuk sebagian besar
# library tersebut, jadi file ini dibiarkan minimal agar mudah diaudit.

# Simpan informasi baris untuk memudahkan pelaporan crash saat presentasi.
-keepattributes SourceFile,LineNumberTable
