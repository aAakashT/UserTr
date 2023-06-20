from django.shortcuts import render, redirect
from rest_framework.views import APIView
from rest_framework.viewsets import ModelViewSet
from rest_framework.authentication import SessionAuthentication
from rest_framework.permissions import AllowAny
from rest_framework import generics , permissions
from usertrainerapp.serializers import UserSerializer, RegisterSerializer
from rest_framework import status
from rest_framework.response import Response 
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.models import Group
from usertrainerapp.models import User

class UserRegistration(ModelViewSet):
    serializer_class = RegisterSerializer
    queryset = User.objects.all

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data = request.data)
        serializer.is_valid(raise_exception = True)
        group = Group.objects.get(name = 'User')
        self.perform_create(serializer)
        user = serializer.instance
        user.groups.add(group)
        headers = self.get_success_headers(serializer.data)
        return Response(serializer.data, status = status.HTTP_201_CREATED, headers=headers)

class LoginApi(APIView):
    
    def post(self, request):
        # print(request.session.get('csrftoken'))
        email = request.data.get('email')
        # print(email)
        password = request.data.get('password')
        username = request.data.get('username')
        # print(password)
        user = User.objects.get(email=email)
        print(user)

        # print(user)
        # user = authenticate(request, username=username, password=password, email=email)
        # print(user)
        if user:
            login(request, user)
            session_key = request.session.session_key
            request.session.set_expiry(0)
            return Response({'success': True}, status=status.HTTP_200_OK)
        return Response({'error': 'Invalid credentials'}, status=status.HTTP_401_UNAUTHORIZED)



class LogoutApi(APIView):
    def post(self, request):
        logout(request)
        return Response(status=status.HTTP_200_OK)


def login_view(request):
    if request.user.is_authenticated:
        return render(request, 'dashboard.html')  
    return render(request, 'auth_templates/login.html')

def register_view(request):
    if request.user.is_authenticated:
        return render(request, 'dashboard.html')  
    return render(request, 'auth_templates/register.html')
