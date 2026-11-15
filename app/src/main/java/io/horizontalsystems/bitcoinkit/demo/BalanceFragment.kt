
package io.horizontalsystems.bitcoinkit.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bitcoincore.BitcoinCore
import java.text.SimpleDateFormat
import java.util.*

class BalanceFragment : Fragment() {

    lateinit var viewModel: MainViewModel
    lateinit var networkName: TextView
    lateinit var balanceValue: TextView
    lateinit var balanceUnspendableValue: TextView
    lateinit var lastBlockDateValue: TextView
    lateinit var lastBlockValue: TextView
    lateinit var lastBlockHashValue: TextView
    lateinit var stateValue: TextView
    lateinit var startButton: Button
    lateinit var clearButton: Button
    lateinit var buttonDebug: Button
    lateinit var buttonStatus: Button
    lateinit var buttonCopyHash: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.let {
            ViewModelProvider(it).get(MainViewModel::class.java)
        } ?: return

        viewModel.balance.observe(this, Observer { balance ->
            when (balance) {
                null -> {
                    balanceValue.text = ""
                    balanceUnspendableValue.text = ""
                }
                else -> {
                    balanceValue.text =
                        NumberFormatHelper.cryptoAmountFormat.format(
                            balance.spendable / 100_000_000.0
                        )

                    balanceUnspendableValue.text =
                        NumberFormatHelper.cryptoAmountFormat.format(
                            (balance.unspendableTimeLocked +
                                    balance.unspendableNotRelayed) / 100_000_000.0
                        )
                }
            }
        })

        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        )

        viewModel.lastBlock.observe(this, Observer { blockInfo ->
            blockInfo?.let {
                lastBlockValue.text = it.height.toString()
                lastBlockHashValue.text = it.headerHash

                val strDate = dateFormat.format(
                    Date(it.timestamp * 1000)
                )

                lastBlockDateValue.text = strDate
            }
        })

        viewModel.state.observe(this, Observer { state ->
            when (state) {
                is BitcoinCore.KitState.Synced -> {
                    stateValue.text = "synced"
                }
                is BitcoinCore.KitState.ApiSyncing -> {
                    stateValue.text = "api syncing ${state.transactions} txs"
                }
                is BitcoinCore.KitState.Syncing -> {
                    stateValue.text =
                        "syncing ${"%.3f".format(state.progress)}"
                }
                is BitcoinCore.KitState.NotSynced -> {
                    stateValue.text =
                        "not synced ${state.exception.javaClass.simpleName}"
                }
            }
        })

        viewModel.status.observe(this, Observer {
            when (it) {
                MainViewModel.State.STARTED -> {
                    startButton.isEnabled = false
                }
                else -> {
                    startButton.isEnabled = true
                }
            }
        })

        viewModel.statusInfo.observe(this, Observer { statusInfo ->
            activity?.let {
                val dialog = AlertDialog.Builder(it)
                    .setMessage(formatMapToString(statusInfo))
                    .setTitle("Status Info")
                    .create()

                dialog.show()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_balance,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        networkName = view.findViewById(R.id.networkName)
        networkName.text = viewModel.networkName

        balanceValue = view.findViewById(R.id.balanceValue)
        balanceUnspendableValue =
            view.findViewById(R.id.balanceUnspendableValue)

        lastBlockValue = view.findViewById(R.id.lastBlockValue)
        lastBlockDateValue = view.findViewById(R.id.lastBlockDateValue)
        lastBlockHashValue = view.findViewById(R.id.lastBlockHashValue)

        stateValue = view.findViewById(R.id.stateValue)

        startButton = view.findViewById(R.id.buttonStart)
        clearButton = view.findViewById(R.id.buttonClear)
        buttonDebug = view.findViewById(R.id.buttonDebug)
        buttonStatus = view.findViewById(R.id.buttonStatus)
        buttonCopyHash = view.findViewById(R.id.buttonCopyHash)

        buttonCopyHash.setOnClickListener {
            val hash = lastBlockHashValue.text.toString()

            if (hash.isNotBlank()) {
                val clipboard = requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager

                val clip = ClipData.newPlainText(
                    "Bitcoin Block Hash",
                    hash
                )

                clipboard.setPrimaryClip(clip)

                Toast.makeText(
                    requireContext(),
                    "Block hash copied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Block hash is not available yet",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        startButton.setOnClickListener {
            viewModel.start()
        }

        clearButton.setOnClickListener {
            viewModel.clear()
        }

        buttonDebug.setOnClickListener {
            viewModel.showDebugInfo()
        }

        buttonStatus.setOnClickListener {
            viewModel.showStatusInfo()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun formatMapToString(
        status: Map<String, Any>?,
        indentation: String = "",
        bullet: String = "",
        level: Int = 0
    ): String? {

        if (status == null) return null

        val sb = StringBuilder()

        status.toList().forEach { (key, value) ->
            val title = "$indentation$bullet$key"

            when (value) {
                is Map<*, *> -> {
                    val formattedValue = formatMapToString(
                        value as? Map<String, Any>,
                        "\t\t$indentation",
                        " - ",
                        level + 1
                    )

                    sb.append(
                        "$title:\n$formattedValue${if (level < 2) "\n" else ""}"
                    )
                }
                else -> {
                    sb.appendln("$title: $value")
                }
            }
        }

        val statusString = sb.trimEnd()

        return if (statusString.isEmpty()) {
            ""
        } else {
            "$statusString\n"
        }
    }
}
