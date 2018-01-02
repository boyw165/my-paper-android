package com.paper.protocol

import com.paper.MyRouter
import ru.terrakok.cicerone.NavigatorHolder

interface IRouterProvider {

    val router: MyRouter

    val holder: NavigatorHolder
}