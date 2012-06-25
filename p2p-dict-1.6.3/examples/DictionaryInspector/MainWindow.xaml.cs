using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Windows.Threading;
using GroupLab.PeerDictionary;

namespace DictionaryInspector
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        P2PDictionary mDictionary;
        DispatcherTimer mTimer;

        public MainWindow(string ns, int portHint)
        {
            InitializeComponent();

            mDictionary = new P2PDictionary("Dictionary Inspector", P2PDictionary.GetFreePort(portHint),
                ns, P2PDictionaryServerMode.AutoRegister, P2PDictionaryClientMode.AutoConnect);
            mDictionary.AddSubscription("*");

            mTimer = new DispatcherTimer();
            mTimer.Interval = new TimeSpan(0, 0, 0, 1);
            mTimer.Tick += new EventHandler(mTimer_Tick);
            mTimer.Start();

            this.Title = "Dictionary Inspector (ns: " + ns + ") port " + ((System.Net.IPEndPoint)mDictionary.LocalEndPoint).Port;

            dataGrid.ItemsSource = new List<KeyValuePair<string,object>>( mDictionary);
            
        }

        void mTimer_Tick(object sender, EventArgs e)
        {
            // regrab all keys from the dictionary
            dataGrid.ItemsSource = new List<KeyValuePair<string, object>>(mDictionary);
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            mTimer.Stop();
        }
    }
}
