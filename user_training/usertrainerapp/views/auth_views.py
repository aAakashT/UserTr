from django.shortcuts import render, redirect
from rest_framework.views import APIView
from rest_framework.viewsets import ModelViewSet
from usertrainerapp.serializers import UserSerializer, RegisterSerializer
from rest_framework import status
from rest_framework.response import Response 
from django.contrib.auth import login, logout
from django.contrib.auth.models import Group
from usertrainerapp.models import User
from rest_framework.renderers import TemplateHTMLRenderer
from django.urls import reverse_lazy
from django.shortcuts import render
from django.contrib.auth.decorators import login_required
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
            if request.user.is_authenticated:
                print("yes")
                groups = request.user.groups.all()
                if groups.filter(name='Admin').exists():
                    return redirect('admin_dashboard')
                elif groups.filter(name='Team_Leader').exists():
                    return redirect('tl_dashboard')
                elif groups.filter(name='User').exists():
                    return redirect('user_dashboard')
            # group = user.groups.first()
            # ad_group, _ = Group.objects.get_or_create("Admin")
            # tl_group, _ = Group.objects.filte("Team_Lead")
            # user_group, _ = Group.objects.get("User")
            # print(type(tl_group))
            # print(group.name)
            # if group.name == 'Team_Leader':
            #     return redirect('tl_dashboard')  
            # elif group.name == 'Admin':
            #     return redirect('admin_dashboard')  
            # elif group.name == 'User':
            #     return redirect('user_dashboard') 

            session_key = request.session.session_key
            request.session.set_expiry(0)
            return redirect(reverse_lazy('dashboard'))

        return Response({'error': 'Invalid credentials'}, status=status.HTTP_401_UNAUTHORIZED)



class LogoutApi(APIView):
    template_name = 'auth_templates/logout.html'

    def get(self, request):
        return render(request, self.template_name)

    def post(self, request):
        logout(request)
        return redirect(reverse_lazy('login'))


def login_view(request):
    if request.user.is_authenticated:
        return redirect(reverse_lazy('dashboard'))
    return render(request, 'auth_templates/login.html')

def register_view(request):
    if request.user.is_authenticated:
        return redirect(reverse_lazy('dashbord'))
    return render(request, 'auth_templates/register.html')


def dashboard_view(request):
    return render(request, 'dashboard.html')

def handle_redirect(request):
    if request.user.is_authenticated:
        print("yes")
        groups = request.user.groups.all()
        if groups.filter(name='Admin').exists():
            return redirect('admin_dashboard')
        elif groups.filter(name='Team_Leader').exists():
            return redirect('tl_dashboard')
        elif groups.filter(name='User').exists():
            return redirect('user_dashboard')
        else:
            redirect('dashboard')