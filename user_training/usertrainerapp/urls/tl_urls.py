"""
URL configuration for user_training project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/4.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from usertrainerapp.views.tl_views import  api_get_users, tl_dashboard_view, render_users,api_get_modules,ViewReview, UpdateReview, DeleteReview, WriteReview, AssignModules, render_modules
from django.urls import path
urlpatterns = [
    path('get/users/', api_get_users, name = 'get_users'),
    path('get/modules/', api_get_modules, name = 'get_modules'),
    path('render/modules/', render_modules, name = 'render_modules'),
    path('render/users/', render_users, name = 'render_users'),
    path('assign/modules/<int:user_id>/', AssignModules.as_view(), name = 'assign_module'),
    path('review/<int:user_id>/', WriteReview.as_view(), name='write_review'),
    path('review/update/<int:review_id>/', UpdateReview.as_view(), name='update_review'),
    path('review/delete/<int:review_id>/', DeleteReview.as_view(), name='delete_review'),
    path('reviews/<int:review_id>/', ViewReview.as_view(), name='view_review'),
    path('tl/dashboard/', tl_dashboard_view , name = 'tl_dashboard'),

]
