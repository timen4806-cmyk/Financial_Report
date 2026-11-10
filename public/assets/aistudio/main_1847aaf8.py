import os
import sqlite3
import webbrowser
import urllib.parse
from datetime import datetime
from kivy.lang import Builder
from kivy.metrics import dp
from kivy.utils import platform
from kivy.uix.screenmanager import ScreenManager, Screen
from kivymd.app import MDApp
from kivymd.uix.datatables import MDDataTable
from kivymd.uix.dialog import MDDialog
from kivymd.uix.button import MDRaisedButton, MDFlatButton
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.textfield import MDTextField
from kivymd.uix.pickers import MDDatePicker
from kivymd.uix.filemanager import MDFileManager
from kivymd.uix.menu import MDDropdownMenu
from kivymd.uix.list import OneLineListItem
from kivymd.toast import toast
from fpdf import FPDF

if platform == 'android':
    from android.permissions import request_permissions, Permission
    from android.storage import app_storage_path
    try:
        INTERNAL_PATH = app_storage_path()
    except:
        INTERNAL_PATH = os.path.dirname(os.path.abspath(__file__))
    
    DB_NAME = os.path.join(INTERNAL_PATH, "finance_reports_v13.db")
    BASE_DOWNLOAD_PATH = "/sdcard/Download/Financial_Reports"
else:
    DB_NAME = "finance_reports_v13.db"
    BASE_DOWNLOAD_PATH = 'Financial_Reports'

def init_db():
    try:
        conn = sqlite3.connect(DB_NAME)
        c = conn.cursor()
        c.execute('''CREATE TABLE IF NOT EXISTS kas 
                     (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT, uraian TEXT, 
                      masuk INTEGER, keluar INTEGER, grup TEXT)''')
        c.execute('''CREATE TABLE IF NOT EXISTS users (username TEXT, password TEXT)''')
        c.execute("SELECT * FROM users")
        if not c.fetchone():
            c.execute("INSERT INTO users VALUES (?,?)", ("admin", "admin234"))
        conn.commit()
        conn.close()
    except Exception as e: 
        print(f"DB Error: {e}")

def format_rp(angka):
    try: 
        return f"Rp. {int(angka):,.0f}".replace(",", ".")
    except: 
        return "Rp. 0"

KV = '''
ScreenManager:
    LoginScreen:
    MainScreen:
    InputScreen:

<LoginScreen>:
    name: 'login'
    MDFloatLayout:
        md_bg_color: 0.95, 0.95, 0.95, 1
        MDCard:
            size_hint: 0.85, 0.65
            pos_hint: {"center_x": .5, "center_y": .5}
            elevation: 4
            padding: dp(25)
            spacing: dp(15)
            orientation: 'vertical'
            radius: [20, ]
            MDIcon:
                icon: "shield-account"
                halign: "center"
                font_size: "80sp"
                theme_text_color: "Custom"
                text_color: app.theme_cls.primary_color
            MDLabel:
                text: "APLIKASI FINANCIAL REPORTS HZdotCOM"
                halign: "center"
                font_style: "H5"
                bold: True
            MDTextField:
                id: user
                hint_text: "Username"
                icon_left: "account"
                mode: "rectangle"
            MDTextField:
                id: pw
                hint_text: "Password"
                icon_left: "key"
                mode: "rectangle"
                password: True
            MDRaisedButton:
                text: "LOGIN APLIKASI"
                pos_hint: {"center_x": .5}
                size_hint_x: 1
                on_release: app.proses_login()

<MainScreen>:
    name: 'main'
    MDBoxLayout:
        orientation: 'vertical'
        padding: dp(10)
        spacing: dp(10)
        MDTopAppBar:
            id: toolbar
            title: "Laporan Keuangan HZdotCOM"
            elevation: 4
            right_action_items: 
                [["folder-plus", lambda x: app.show_group_menu(x)], 
                ["account-cog", lambda x: app.dialog_ganti_pw()], 
                ["logout", lambda x: app.logout()]]
        
        MDBoxLayout:
            id: table_container
            orientation: 'vertical'
            
        MDCard:
            adaptive_height: True
            padding: dp(10)
            md_bg_color: 0.1, 0.5, 0.5, 0.1
            MDLabel:
                id: label_summary
                text: "Memuat..."
                halign: "center"
                bold: True
                
        MDBoxLayout:
            adaptive_height: True
            spacing: dp(5)
            padding: dp(5)
            MDRaisedButton:
                text: "+ DATA"
                md_bg_color: "#4CAF50"
                on_release: 
                    app.buka_halaman_input()
                    root.manager.current = 'input_page'
            MDRaisedButton:
                text: "LOGO"
                md_bg_color: "#2196F3"
                on_release: app.open_file_manager()
            MDRaisedButton:
                text: "PDF"
                md_bg_color: "#F44336"
                on_release: app.dialog_ekspor()
            MDRaisedButton:
                text: "HAPUS"
                md_bg_color: "#9E9E9E"
                on_release: app.hapus_data()

<InputScreen>:
    name: 'input_page'
    MDBoxLayout:
        orientation: 'vertical'
        padding: dp(10)
        spacing: dp(10)
        MDTopAppBar:
            title: "Input & Edit Data"
            elevation: 4
            left_action_items: [["arrow-left", lambda x: app.kembali_ke_main()]]
        
        MDCard:
            orientation: "vertical"
            padding: dp(16)
            spacing: dp(12)
            elevation: 2
            size_hint_y: None
            height: self.minimum_height
            
            MDTextField:
                id: input_uraian
                hint_text: "Uraian Transaksi"
                mode: "rectangle"
            MDTextField:
                id: input_masuk
                hint_text: "Jumlah Pemasukan (Rp)"
                input_filter: "int"
                mode: "rectangle"
            MDTextField:
                id: input_keluar
                hint_text: "Jumlah Pengeluaran (Rp)"
                input_filter: "int"
                mode: "rectangle"
            
            MDBoxLayout:
                adaptive_height: True
                spacing: dp(10)
                padding: [0, dp(10), 0, 0]
                MDRaisedButton:
                    id: btn_tgl
                    text: "PILIH TANGGAL"
                    md_bg_color: "#607D8B"
                    on_release: app.show_date_picker()
                MDLabel:
                    id: lbl_tgl_terpilih
                    text: "Tanggal: -"
                    valign: "center"
                    bold: True
            
            MDBoxLayout:
                adaptive_height: True
                spacing: dp(10)
                padding: [0, dp(20), 0, 0]
                MDRaisedButton:
                    id: btn_simpan
                    text: "SIMPAN DATA"
                    md_bg_color: "#4CAF50"
                    on_release: app.tambah_data()
                MDRaisedButton:
                    id: btn_edit
                    text: "UPDATE DATA"
                    disabled: True
                    md_bg_color: "#FF9800"
                    on_release: app.update_data()
                MDFlatButton:
                    text: "RESET DATA"
                    on_release: app.reset_form()
        MDWidget:
'''

class LoginScreen(Screen): 
    pass

class MainScreen(Screen): 
    pass

class InputScreen(Screen):
    pass

class LaporanKeuanganHZdotCOMApp(MDApp):
    def build(self):
        self.theme_cls.primary_palette = "Teal"
        self.active_grup = "Laporan Keuangan"
        self.selected_id = None
        self.logo_path = None
        self.selected_date = datetime.now().strftime("%Y-%m-%d")
        init_db()
        self.screen_manager = Builder.load_string(KV)
        return self.screen_manager

    def on_start(self):
        if platform == 'android':
            from android.permissions import request_permissions, Permission
            request_permissions([Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE])
        self.file_manager = MDFileManager(exit_manager=self.exit_manager, select_path=self.select_logo_path, preview=True)

    def proses_login(self):
        u = self.screen_manager.get_screen('login').ids.user.text
        p = self.screen_manager.get_screen('login').ids.pw.text
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM users WHERE username=? AND password=?", (u, p))
        if cursor.fetchone():
            self.screen_manager.current = 'main'
            self.init_table()
            self.load_data()
        else: 
            toast("Login Gagal!")
        conn.close()

    def buka_halaman_input(self):
        self.reset_form()
        input_screen = self.screen_manager.get_screen('input_page')
        input_screen.ids.lbl_tgl_terpilih.text = f"Tanggal: {self.selected_date}"

    def kembali_ke_main(self):
        self.screen_manager.current = 'main'
        self.load_data()

    def init_table(self):
        container = self.screen_manager.get_screen('main').ids.table_container
        container.clear_widgets()
        
        layout_tabel = MDBoxLayout(orientation="vertical", spacing=dp(5))
        
        self.search_field = MDTextField(
            hint_text="Cari berdasarkan Uraian / Tanggal...",
            icon_left="magnify",
            size_hint_x=1,
            helper_text="Ketik untuk memfilter data otomatis",
            helper_text_mode="on_focus"
        )
        self.search_field.bind(text=self.filter_data_tabel)
        layout_tabel.add_widget(self.search_field)
        
        self.data_table = MDDataTable(
            size_hint=(1, 1), 
            check=True,
            use_pagination=True,
            rows_num=8,
            column_data=[
                ("No", dp(17)), ("Tanggal", dp(25)), ("Uraian", dp(40)), 
                ("Pemasukan", dp(25)), ("Pengeluaran", dp(25)), ("Saldo", dp(30))
            ]
        )
        self.data_table.bind(on_row_press=self.on_row_press)
        layout_tabel.add_widget(self.data_table)
        container.add_widget(layout_tabel)

    def load_data(self):
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM kas WHERE grup=? ORDER BY tanggal ASC", (self.active_grup,))
        rows = cursor.fetchall()
        conn.close()
        
        self.all_table_rows = []
        s_akhir, tm, tk = 0, 0, 0
        for r in rows:
            s_akhir += (r[3] - r[4])
            tm += r[3]
            tk += r[4]
            self.all_table_rows.append((str(r[0]), r[1], r[2], format_rp(r[3]), format_rp(r[4]), format_rp(s_akhir)))
            
        self.data_table.row_data = self.all_table_rows
        self.screen_manager.get_screen('main').ids.label_summary.text = f"SALDO AKHIR: {format_rp(tm - tk)}"

    def filter_data_tabel(self, instance, value):
        if not value:
            self.data_table.row_data = self.all_table_rows
            return
        
        query = value.lower()
        filtered_rows = []
        for row in self.all_table_rows:
            if query in row[1].lower() or query in row[2].lower():
                filtered_rows.append(row)
        self.data_table.row_data = filtered_rows

    def show_group_menu(self, caller):
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT DISTINCT grup FROM kas")
        rows = cursor.fetchall()
        conn.close()
        menu_items = [{"viewclass": "OneLineListItem", "text": "+ BUAT JUDUL BARU", "on_release": lambda: self.dialog_buat_grup_baru()}]
        for r in rows:
            if r[0]: 
                menu_items.append({"viewclass": "OneLineListItem", "text": r[0], "on_release": lambda x=r[0]: self.switch_grup(x)})
        self.group_menu = MDDropdownMenu(caller=caller, items=menu_items, width_mult=4)
        self.group_menu.open()

    def dialog_buat_grup_baru(self):
        self.group_menu.dismiss()
        content = MDBoxLayout(orientation="vertical", spacing="12dp", adaptive_height=True)
        self.g_input = MDTextField(hint_text="Nama Judul Baru")
        content.add_widget(self.g_input)
        self.g_dial = MDDialog(
            title="Judul Baru", 
            type="custom", 
            content_cls=content,
            buttons=[MDRaisedButton(text="OK", on_release=lambda x: self.switch_grup(self.g_input.text))]
        )
        self.g_dial.open()

    def switch_grup(self, grup_name):
        if hasattr(self, 'group_menu'): self.group_menu.dismiss()
        if hasattr(self, 'g_dial'): self.g_dial.dismiss()
        self.active_grup = grup_name.upper() if grup_name else "LAPORAN KEUANGAN"
        self.screen_manager.get_screen('main').ids.toolbar.title = f"Judul Baru:{self.active_grup}"
        self.load_data()

    def tambah_data(self):
        ids = self.screen_manager.get_screen('input_page').ids
        try:
            u, m, k = ids.input_uraian.text, int(ids.input_masuk.text or 0), int(ids.input_keluar.text or 0)
            if not u: return
            conn = sqlite3.connect(DB_NAME)
            conn.execute("INSERT INTO kas (tanggal, uraian, masuk, keluar, grup) VALUES (?,?,?,?,?)", (self.selected_date, u, m, k, self.active_grup))
            conn.commit()
            conn.close()
            self.kembali_ke_main()
            toast("Data Berhasil Disimpan!")
        except Exception as e: 
            toast("Gagal menyimpan!")

    def update_data(self):
        ids = self.screen_manager.get_screen('input_page').ids
        try:
            u, m, k = ids.input_uraian.text, int(ids.input_masuk.text or 0), int(ids.input_keluar.text or 0)
            conn = sqlite3.connect(DB_NAME)
            conn.execute("UPDATE kas SET tanggal=?, uraian=?, masuk=?, keluar=? WHERE id=?", (self.selected_date, u, m, k, self.selected_id))
            conn.commit()
            conn.close()
            self.kembali_ke_main()
            toast("Data Berhasil Diperbarui!")
        except Exception as e: 
            toast("Update gagal!")

    def on_row_press(self, instance_table, instance_row):
        row_num = int(instance_row.index / len(instance_table.column_data))
        row_data = instance_table.row_data[row_num]        
        ids = self.screen_manager.get_screen('input_page').ids
        self.selected_id, self.selected_date = row_data[0], row_data[1]        
        ids.lbl_tgl_terpilih.text = f"Tanggal: {self.selected_date}"
        ids.input_uraian.text = row_data[2]
        ids.input_masuk.text = row_data[3].replace("Rp. ", "").replace(".", "")
        ids.input_keluar.text = row_data[4].replace("Rp. ", "").replace(".", "")        
        ids.btn_edit.disabled, ids.btn_simpan.disabled = False, True        
        self.screen_manager.current = 'input_page'

    def reset_form(self):
        self.selected_id = None
        self.selected_date = datetime.now().strftime("%Y-%m-%d")        
        ids = self.screen_manager.get_screen('input_page').ids
        ids.lbl_tgl_terpilih.text = f"Tanggal: {self.selected_date}"
        ids.input_uraian.text = ""
        ids.input_masuk.text = ""
        ids.input_keluar.text = ""
        ids.btn_edit.disabled, ids.btn_simpan.disabled = True, False

    def hapus_data(self):
        checks = self.data_table.get_row_checks()
        if not checks: 
            toast("Pilih data melalui checkbox tabel dahulu!")
            return
        conn = sqlite3.connect(DB_NAME)
        for r in checks: 
            conn.execute("DELETE FROM kas WHERE id=?", (r[0],))
        conn.commit()
        conn.close()
        self.load_data()
        toast("Data berhasil dihapus!")

    def show_date_picker(self):
        d = MDDatePicker()
        d.bind(on_save=lambda i, v, r: self.set_date(v))
        d.open()

    def set_date(self, val):
        self.selected_date = val.strftime("%Y-%m-%d")
        self.screen_manager.get_screen('input_page').ids.lbl_tgl_terpilih.text = f"Tanggal: {self.selected_date}"

    def dialog_ganti_pw(self):
        content = MDBoxLayout(orientation="vertical", spacing="12dp", adaptive_height=True)
        self.nu = MDTextField(hint_text="User Baru")
        self.np = MDTextField(hint_text="Password Baru", password=True)
        content.add_widget(self.nu)
        content.add_widget(self.np)
        self.pw_dial = MDDialog(
            title="Update Username & Password", 
            type="custom", 
            content_cls=content,
            buttons=[MDRaisedButton(text="SIMPAN DATA", on_release=lambda x: self.save_pw())]
        )
        self.pw_dial.open()

    def save_pw(self):
        if self.nu.text and self.np.text:
            conn = sqlite3.connect(DB_NAME)
            conn.execute("UPDATE users SET username=?, password=?", (self.nu.text, self.np.text))
            conn.commit()
            conn.close()
            toast("Data Tersimpan!")
            self.pw_dial.dismiss()

    def logout(self): 
        self.screen_manager.current = 'login'

    def open_file_manager(self):
        path = "/sdcard" if platform == 'android' else os.path.expanduser("~")
        self.file_manager.show(path)

    def select_logo_path(self, path):
        if os.path.isfile(path) and path.lower().endswith(('.png', '.jpg', '.jpeg', '.ico')):
            self.logo_path = path
            toast("Logo dipilih!")
        self.exit_manager()

    def exit_manager(self, *args):
        self.file_manager.close()

    def dialog_ekspor(self):
        content = MDBoxLayout(orientation="vertical", spacing="8dp", adaptive_height=True)
        self.j_f = MDTextField(hint_text="Judul Laporan", text=f"{self.active_grup}")
        self.p_f = MDTextField(hint_text="Periode (Bulan/Tahun)", text=datetime.now().strftime("%d %B %Y"))        
        content.add_widget(self.j_f)
        content.add_widget(self.p_f)        
        self.d_exp = MDDialog(
            title="Cetak Laporan", 
            type="custom", 
            content_cls=content,
            buttons=[MDRaisedButton(text="PROSES PDF", md_bg_color="#F44336", on_release=lambda x: self.eksekusi_pdf())]
        )
        self.d_exp.open()

    def eksekusi_pdf(self):
        judul, p_manual = self.j_f.text, self.p_f.text
        self.d_exp.dismiss()        
        grup_path = os.path.join(BASE_DOWNLOAD_PATH, self.active_grup)
        if not os.path.exists(grup_path):
            try: 
                os.makedirs(grup_path, exist_ok=True)
            except Exception as e: 
                toast("Gagal membuat folder")
                grup_path = BASE_DOWNLOAD_PATH                
        fpath = os.path.join(grup_path, f"Financial_Reports_{datetime.now().strftime('%H%M%S')}.pdf")        
        try:
            conn = sqlite3.connect(DB_NAME)
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM kas WHERE grup=? ORDER BY tanggal ASC", (self.active_grup,))
            rows = cursor.fetchall()            
            pdf = FPDF()
            pdf.add_page()                        
            if self.logo_path and os.path.exists(self.logo_path):
                try: 
                    pdf.image(self.logo_path, x=97.5, y=15, w=15)
                    pdf.ln(20)
                except: 
                    pdf.ln(10)
            else: 
                pdf.ln(10)            
            pdf.set_font("Arial", 'B', 16)
            pdf.cell(0, 10, judul.upper(), ln=True, align='C')
            pdf.set_font("Arial", '', 11)
            pdf.cell(0, 8, f"Periode: {p_manual}", ln=True, align='C')
            pdf.ln(1)            
            waktu_cetak = datetime.now().strftime("%d-%m-%Y %H:%M:%S")
            pdf.set_font("Arial", 'I', 8)
            pdf.cell(0, 5, f"Dicetak pada tanggal: {waktu_cetak}", ln=True, align='C')            
            pdf.set_line_width(0.8)
            pdf.line(10, pdf.get_y(), 200, pdf.get_y())
            pdf.set_line_width(0.2)
            pdf.line(10, pdf.get_y() + 1.5, 200, pdf.get_y() + 1.5)            
            pdf.ln(8)            
            w = [10, 21, 83, 25, 25, 28]
            pdf.set_font("Arial", 'B', 9)
            pdf.set_fill_color(230, 230, 230)
            headers = ["No", "Tanggal", "Uraian", "Pemasukan", "Pengeluaran", "Saldo"]
            for i, h in enumerate(headers): 
                pdf.cell(w[i], 10, h, 1, 0, 'C', fill=True)
            pdf.ln()            
            pdf.set_font("Arial", size=9)
            s_acc, tm, tk = 0, 0, 0
            for r in rows:
                masuk, keluar = r[3], r[4]
                s_acc += (masuk - keluar)
                tm += masuk
                tk += keluar
                pdf.cell(w[0], 8, str(r[0]), 1, 0, 'C')
                pdf.cell(w[1], 8, str(r[1]), 1, 0, 'C')
                pdf.cell(w[2], 8, str(r[2])[:50], 1)
                pdf.cell(w[3], 8, format_rp(masuk), 1, 0, 'R')
                pdf.cell(w[4], 8, format_rp(keluar), 1, 0, 'R')
                pdf.cell(w[5], 8, format_rp(s_acc), 1, 1, 'R')            
            pdf.set_font("Arial", 'B', 9)
            pdf.set_fill_color(245, 245, 245)
            pdf.cell(w[0]+w[1]+w[2], 10, "SALDO AKHIR", 1, 0, 'C', fill=True)
            pdf.cell(w[3], 10, format_rp(tm), 1, 0, 'R', fill=True)
            pdf.cell(w[4], 10, format_rp(tk), 1, 0, 'R', fill=True)
            pdf.cell(w[5], 10, format_rp(tm-tk), 1, 1, 'R', fill=True)            
            pdf.output(fpath)
            conn.close()
            toast(f"Berhasil: {self.active_grup}")            
            text_wa = f"Finance_Report {self.active_grup} telah selesai dicetak."
            webbrowser.open(f"https://wa.me{urllib.parse.quote(text_wa)}")
            
        except Exception as e: 
            toast(f"Error PDF: {str(e)}")
            if 'conn' in locals(): 
                conn.close()

if __name__ == '__main__':
    LaporanKeuanganHZdotCOMApp().run()
