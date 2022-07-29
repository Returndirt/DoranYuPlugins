package com.yu.plugs

import io.ktor.client.request.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.descriptor.ShortValueArgumentParser.findGroupOrFail
import net.mamoe.mirai.console.command.descriptor.StringValueArgumentParser.findGroupOrFail
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleInput
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.data.RequestEventData
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.math.min

/**
 * 使用 kotlin 版请把
 * `src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `org.example.mirai.plugin.com.yu.plugs.PluginMain` 也就是当前主类全类名
 *
 * 使用 kotlin 可以把 java 源集删除不会对项目有影响
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id和版本，etc
 *s
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 ide 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "com.yu.plugins.MCListener",
        name = "MinecraftServerListener",

        version = "0.1.0"
    ) {
        author("return_dirt")
        info(
            """
            minecraft服务器 群聊消息监听广播
        """.trimIndent()
        )
        // author 和 info 可以删除.
    }
) {

    override fun onEnable() {
        with(logger) {
            info("Plugin loaded")
        }
        //path of config file "${dataFolder.absolutePath}/"



        var microphone:Bot? = null
        var serverDir:String? = null
        var groupNum:Long? = null
        var tarGroup:Group? = null
        var wkDir:String? = null
        var keyRequire:Boolean = false

        var bfW:BufferedWriter? = null

        var listening:Boolean = false

        suspend fun listenServer()
        {
            val pb = ProcessBuilder(serverDir)
            pb.directory(File(wkDir))
            var ps = pb.start()
            var IpS = ps.inputStream
            var IpSReader = InputStreamReader(IpS,"GBK")
            var bfR = BufferedReader(IpSReader)
            var OpS = ps.outputStream
            var OpsWriter = OutputStreamWriter(OpS,"UTF-8")
            bfW = BufferedWriter(OpsWriter)

            listening = true

            while (true)
            {
                val ctt = bfR.readLine().toString()
                try
                {
                    //[00:21:04] [Server thread/INFO]: <Citycake> 怎么开启#号键转发呢
                    //[00:46:58] [Server thread/INFO] [Console/]: <Return_dirt> hi
                    //[01:13:48] [Server thread/INFO]: [Server] only love miku[93]:火锅底料也可以直接吃
                    //r
                    //[01:13:51] [Server thread/INFO]: <Return_dirt> 半夜聊吃的是相当可以
                    //n

                    val cpos:Int = 33+18
                    //val cpos:Int = 33
                    logger.info(ctt)
                    if(ctt.length>cpos)
                    logger.info(ctt[cpos].toString())
                    if(ctt.length>cpos)
                    if(ctt[cpos]=='<')
                    {
                        if(!keyRequire)
                        {
                            tarGroup!!.sendMessage(ctt.substring(cpos))
                        }
                        else
                        {
                            for(i in 0 until ctt.length)
                            {
                                if(ctt[i]=='#')
                                {
                                    tarGroup!!.sendMessage(ctt.substring(cpos).replace("#",""))
                                    continue
                                }
                            }
                        }
                    }
                }
                finally
                {

                }

            }
        }

        fun getwkDir()
        {
            //logger.info("try to find wkDir")
            for(i in serverDir!!.length-1 downTo  0)
            {
                if(serverDir!!.get(i) == '/' || serverDir!!.get(i) == '\\' )
                {
                    wkDir = serverDir!!.substring(0,i)
                    //logger.info("the last / at "+i+" get wkdir as"+wkDir)
                    return
                }
            }
        }

        fun getCfg()
        {
            try
            {
                val cfgFile = File("${dataFolder.absolutePath}/ListenerCFG.txt")
                val args = cfgFile.readLines()
                serverDir = args[0]
                groupNum = args[1].toLong()
                logger.info("serverDir:"+serverDir+" groupNum:"+groupNum.toString())
                getwkDir()
            }
            catch (e:java.io.FileNotFoundException)
            {
                File("${dataFolder.absolutePath}/ListenerCFG.txt").createNewFile()
                logger.info("cfg not found, ${dataFolder.absolutePath}/ListenerCFG.txt created")
                val cfgFile = File("${dataFolder.absolutePath}/ListenerCFG.txt")
                cfgFile.writeText("bat Dir\ngroup ID")
            }
        }
        getCfg()



        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent>
        {

            if(sender.isOperator()&&listening)
            {
                if(message.contentToString()=="/cpm"&&group.id == tarGroup!!.id)
                {
                    keyRequire = !keyRequire
                }
                if(message.contentToString().startsWith("/cdc ")&&group.id == tarGroup!!.id)
                {
                    bfW!!.write(message.contentToString().replace("/cdc ","")+"\n")
                    bfW!!.flush()
                }
            }

            if(listening)
            {
                if(tarGroup!!.id == group.id)
                {
                    //logger.info("microphone online and this message is from the target group")
                    if(message.contentToString().length<50)
                    {
                        //logger.info("short enough")
                        if(!keyRequire)
                        {
                            //logger.info("# is not required")
                            bfW!!.write("say "+sender.nameCardOrNick+"["+sender.id.toString().substring(0,2)+"]:"+message.contentToString()+"\n")
                            bfW!!.flush()
                        }
                        else if (message.contentToString().startsWith('#'))
                        {
                            bfW!!.write("say "+sender.nameCardOrNick+"["+sender.id.toString().substring(0,2)+"]:"+message.contentToString().replace("#","")+"\n")
                            bfW!!.flush()
                            //logger.info("# was found")
                        }
                    }
                }
            }
        }
        eventChannel.subscribeAlways<FriendMessageEvent> {
            //val c = message.contentToString()
            //for(i in 0 until c.length)
            //{
                //logger.info(i.toString()+":"+c[i])
            //}
        }
        eventChannel.subscribeAlways<NewFriendRequestEvent>
        {
        }
        eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {}

        eventChannel.subscribeAlways<BotOnlineEvent>
        {
            logger.info(bot.nick.toString()+"logged in")
            if(microphone==null)
            {
                try
                {
                    bot.getGroupOrFail(groupNum!!)
                    microphone = bot
                    tarGroup = bot.getGroup(groupNum!!)
                    logger.info("mark"+bot.nick.toString()+"as microphone")
                    listenServer()
                }
                catch (e:NoSuchElementException)
                {
                    logger.info("this bot can't connect with target group")
                }
            }
        }


    }
}
