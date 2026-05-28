package com.patrimoniosjc.rfidpoc.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

@SuppressLint("MissingPermission") // Assumimos que as permissões já foram verificadas na MainActivity
class BleManager(
    private val context: Context,
    private val onMessageLog: (String) -> Unit,
    private val onMessageReceived: (String) -> Unit,
    private val onConnectionStateChange: (Boolean) -> Unit
) {
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    
    // Callback para os eventos de escaneamento BLE
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            val deviceName = device?.name
            
            if (deviceName == BleConstants.DEVICE_NAME) {
                onMessageLog("Dispositivo ${BleConstants.DEVICE_NAME} encontrado! Conectando...")
                stopScan()
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            onMessageLog("Falha no escaneamento: Erro $errorCode")
        }
    }

    // Callback para os eventos de conexão GATT
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                onMessageLog("Conectado ao ESP32. Descobrindo serviços...")
                onConnectionStateChange(true)
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onMessageLog("Desconectado do ESP32.")
                onConnectionStateChange(false)
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(BleConstants.SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(BleConstants.CHARACTERISTIC_UUID_RX)
                    txCharacteristic = service.getCharacteristic(BleConstants.CHARACTERISTIC_UUID_TX)
                    
                    if (rxCharacteristic != null && txCharacteristic != null) {
                        onMessageLog("Serviços e características encontradas com sucesso!")
                        enableNotifications(gatt, txCharacteristic!!)
                    } else {
                        onMessageLog("Erro: Características não encontradas.")
                    }
                } else {
                    onMessageLog("Erro: Serviço não encontrado.")
                }
            } else {
                onMessageLog("Falha ao descobrir serviços: $status")
            }
        }

        // Chamado quando o ESP32 envia uma notificação (OK_LED_ON, etc)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleConstants.CHARACTERISTIC_UUID_TX) {
                val receivedData = characteristic.getStringValue(0)
                onMessageReceived(receivedData)
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onMessageLog("Erro: Bluetooth não suportado ou desativado.")
            return
        }

        if (isScanning) return
        
        onMessageLog("Iniciando escaneamento...")
        isScanning = true
        bluetoothLeScanner?.startScan(scanCallback)
        
        // Para o escaneamento após 10 segundos
        handler.postDelayed({
            if (isScanning) {
                stopScan()
                onMessageLog("Tempo de escaneamento esgotado.")
            }
        }, 10000)
    }

    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    // Envia comando "LED_ON" ou "LED_OFF" para a RX
    fun sendCommand(command: String) {
        if (bluetoothGatt == null || rxCharacteristic == null) {
            onMessageLog("Erro: Dispositivo não conectado ou característica indisponível.")
            return
        }

        rxCharacteristic?.value = command.toByteArray()
        bluetoothGatt?.writeCharacteristic(rxCharacteristic)
        onMessageLog("[TX] $command")
    }

    // Configura para ouvir o que o ESP32 envia na TX
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            onMessageLog("Notificações habilitadas. Pronto para receber respostas.")
        }
    }
}
