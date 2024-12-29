import React, { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Header.css';
import { GenderContext } from './GenderContext';

const apiUrl = process.env.REACT_APP_API_URL;

interface HeaderProps {
    onSearch?: (searchTerm: string, genderFilter: string) => void;
}

const Header: React.FC<HeaderProps> = ({ onSearch }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const { genderFilter, setGenderFilter } = useContext(GenderContext);
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [userInfo, setUserInfo] = useState<{ name: string; picture: string } | null>(null);

    useEffect(() => {
        const fetchUserInfo = async () => {
            try {
                const response = await fetch(`${apiUrl}/login/userinfo`, {
                    method: "GET",
                    credentials: "include",
                });
                if (response.ok) {
                    const data = await response.json();
                    setUserInfo({ name: data.name, picture: data.picture });
                    setIsLoggedIn(true);
                } else {
                    setIsLoggedIn(false);
                }
            } catch (error) {
                console.error("사용자 정보를 가져오는 중 오류 발생:", error);
                setIsLoggedIn(false);
            }
        };

        fetchUserInfo();
    }, []);

    const handleLogin = () => {
        window.location.href = `${apiUrl}/oauth2/authorization/google`;
    };

    const handleLogout = () => {
        setUserInfo(null);
        setIsLoggedIn(false);
        alert("로그아웃되었습니다.");
    };

    const handleSearch = () => {
        const trimmedSearchTerm = searchTerm.trim();
        if (trimmedSearchTerm.length < 2) {
            alert('검색어는 두 글자 이상이 필요합니다.');
            return;
        }

        if (onSearch) {
            onSearch(searchTerm, genderFilter);
        } else {
            navigate(`/search?query=${encodeURIComponent(searchTerm)}&gender=${genderFilter}`);
        }
    };

    const handleKeyPress = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            handleSearch();
        }
    };

    return (
        <header className="header">
            <div style={{ display: 'flex', alignItems: 'center' }}>
                <img
                    src="/images/V-Song.png"
                    alt="V-Song Logo"
                    className="logo"
                    onClick={() => navigate('/')}
                />
                {isLoggedIn ? (
                    <div className="user-info">
                        <img src={userInfo?.picture} alt="User" className="user-avatar" />
                        <span>{userInfo?.name}</span>
                        <button className="logout-btn" onClick={handleLogout}>
                            로그아웃
                        </button>
                    </div>
                ) : (
                    <button className="login-btn" onClick={handleLogin}>
                        로그인
                    </button>
                )}
            </div>
            <div className="search-bar">
                <input
                    type="text"
                    placeholder="검색"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={handleKeyPress}
                />
                <button className="search-btn" onClick={handleSearch}>
                    <img src="/images/SearchBar.png" alt="Search" />
                </button>
            </div>
            <div className="gender-filters">
                <button
                    className={`gender-btn ${genderFilter === 'male' ? 'active' : ''}`}
                    onClick={() => setGenderFilter('male')}
                >
                    남성
                </button>
                <button
                    className={`gender-btn ${genderFilter === 'female' ? 'active' : ''}`}
                    onClick={() => setGenderFilter('female')}
                >
                    여성
                </button>
                <button
                    className={`gender-btn ${genderFilter === 'all' ? 'active' : ''}`}
                    onClick={() => setGenderFilter('all')}
                >
                    전체
                </button>
            </div>
        </header>
    );
};

export default Header;
